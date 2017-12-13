#!/usr/bin/python
import logging
import sys
import argparse
import time
import json
import dateutil.parser as time_parser
import requests
from requests.packages.urllib3.util.retry import Retry
from requests.adapters import HTTPAdapter

logging.basicConfig()
logger = logging.getLogger('export_data')
logger.setLevel(logging.INFO)

class ExportData(object):
    ''' Export Data class
    This class handles real time data collection from the source splunk server with given
    index and sourcetype and export the events to destionation splunk server
    '''
    def __init__(self, src, dest, dest_token, index, source_types, time_window):
        self.src = src
        self.dest = dest
        self.dest_token = dest_token
        self.index = index
        self.source_types = source_types
        self.time_window = time_window

        self.source_admin_user = 'admin'
        self.source_admin_password = 'changed'
        self.dest_admin_user = 'admin'
        self.dest_admin_password = 'changed'
        self.timeout = 30

    # pylint disable=no-self-use
    def check_request_status(self, req_obj):
        '''
        check if a request is successful
        @param: req_obj
        returns True/False
        '''
        if not req_obj.ok:
            logger.error(str(req_obj.status_code) + '\n' + req_obj.text)
            return False
        return True

    def check_source_connection(self):
        '''
        check if a source server connection is accessible
        returns True/False
        '''
        service_url = self.src + '/services'
        logger.info('requesting: ' + service_url)
        try:
            res = self.requests_retry_session().get(
                service_url,
                auth=(self.source_admin_user, self.source_admin_password),
                verify=False)
            if not self.check_request_status(res):
                raise Exception('source server is not accessible.')
        except Exception:
            logger.exception('unable to establish a connection with source server')
            return False

        return True

    def check_dest_connection(self):
        '''
        check if a destination server connection is accessible by sending a test event
        returns True/False
        '''
        try:
            dest_url = self.dest + '/services/collector/event'
            logger.info('requesting: ' + dest_url)
            headers = {
                'Authorization': 'Splunk {token}'.format(token=self.dest_token),
                'Content-Type': 'application/json',
            }
            data = {
                'event': 'test',
            }
            res = self.requests_retry_session().post(dest_url, headers=headers,
                                                     data=json.dumps(data), verify=False)
            if not self.check_request_status(res):
                raise Exception('destionation server is not accessible.')
        except Exception:
            logger.exception('unable to post an event to destination server')
            return False

        return True

    def compose_search_query(self):
        '''
        compose a splunk search query with input index and source types
        returns job_str
        '''
        for idx, item in enumerate(self.source_types):
            self.source_types[idx] = 'sourcetype=' + item

        source_type_str = ' OR '.join(self.source_types)
        job_str = 'search index={index} {source_type_search}' \
                   .format(index=self.index, source_type_search=source_type_str)

        return job_str

    def collect_data(self, query, start_time, end_time):
        '''
        collect events from the source server
        @param: query (search query)
        @param: start_time (search start time)
        @param: end_time (search end time)
        returns events
        '''
        url = self.src + '/services/search/jobs?output_mode=json'
        logger.info('requesting: ' + url)
        data = {
            'search': query,
            'earliest_time': start_time,
            'latest_time': end_time,
        }
        try:
            create_job = self.requests_retry_session().post(
                url,
                auth=(self.source_admin_user, self.source_admin_password),
                verify=False, data=data)
            if not self.check_request_status(create_job):
                raise Exception('Failed to create search job: ' + url)

            json_res = create_job.json()
            job_id = json_res['sid']
            events = self.wait_for_job_and_get_events(job_id)

        except Exception:
            logger.exception('Failed to collect data.')

        return events

    def wait_for_job_and_get_events(self, job_id):
        '''
        wait for the search job to finish and collect the result events
        @param: job_id
        returns events
        '''
        events = []
        job_url = self.src + '/services/search/jobs/' + str(job_id) + '?output_mode=json'
        logger.info('requesting: ' + job_url)

        try:
            for _ in range(self.timeout):
                res = self.requests_retry_session().get(
                    job_url,
                    auth=(self.source_admin_user, self.source_admin_password),
                    verify=False)
                if not self.check_request_status(res):
                    raise Exception('Search job: {0} is invalid'.format(job_url))

                job_res = res.json()
                dispatch_state = job_res['entry'][0]['content']['dispatchState']
                if dispatch_state == 'DONE':
                    events = self.get_events(job_id)
                    break
                if dispatch_state == 'FAILED':
                    raise Exception('Search job: {0} failed'.format(job_url))
                time.sleep(1)
        except Exception:
            logger.exception('Failed to run search job.')

        return events

    def get_events(self, job_id):
        '''
        collect the result events from a search job
        @param: job_id
        returns events
        '''
        event_url = self.src + '/services/search/jobs/' + str(job_id) + '/events/?output_mode=json'
        logger.info('requesting: ' + event_url)
        try:
            event_job = self.requests_retry_session().get(
                event_url, auth=(self.source_admin_user, self.source_admin_password), verify=False)
            if not self.check_request_status(event_job):
                raise Exception('Failed to get events for search: {0}'.format(event_url))

            event_job_json = event_job.json()
            events = event_job_json['results']
        except Exception:
            logger.exception('Search job %s failed to return events.', job_id)

        return events

    def send_to_dest_thru_hec(self, events):
        '''
        send collected events to the destination server
        @param: events
        '''
        if not events:
            logger.info('No events collected.')
            return

        dest_url = self.dest + '/services/collector/event'
        logger.info('sending data to : ' + dest_url)
        post_obj = []
        for event in events:
            temp = {}
            temp['event'] = event['_raw']
            parsed_t = time_parser.parse(event['_time'])
            temp['time'] = parsed_t.strftime('%s')
            temp['host'] = event['host']
            temp['source'] = event['source']
            temp['sourcetype'] = event['sourcetype']
            post_obj.append(temp)
        data = '\n'.join(json.dumps(event) for event in post_obj)
        headers = {
            'Authorization': 'Splunk {token}'.format(token=self.dest_token),
            'Content-Type': 'application/json',
        }
        try:
            res = self.requests_retry_session().post(
                dest_url, verify=False, headers=headers, data=data)
            if not self.check_request_status(res):
                raise Exception('Failed to post events with request: %s', dest_url)
        except Exception:
            logger.exception('Failed to post events to the dest.')

    def requests_retry_session(
            self,
            retries=10,
            backoff_factor=0.1,
            status_forcelist=(500, 502, 504),
    ):
        '''
        create a retry session for HTTP/HTTPS requests
        @param: retries (num of retry time)
        @param: backoff_factor
        @param: status_forcelist (list of error status code to trigger retry)
        @param: session
        returns: session
        '''
        session = requests.Session()
        retry = Retry(
            total=int(retries),
            backoff_factor=backoff_factor,
            method_whitelist=frozenset(['GET', 'POST']),
            status_forcelist=status_forcelist,
        )
        adapter = HTTPAdapter(max_retries=retry)
        session.mount('http://', adapter)
        session.mount('https://', adapter)

        return session

    def run(self):
        '''
        function to run data collection and export
        '''
        if not self.check_source_connection():
            logger.error('source is not accessible')
            sys.exit(0)

        if not self.check_dest_connection():
            logger.error('dest is not accessible')
            sys.exit(0)

        end_time = time.time()
        query = self.compose_search_query()
        time.sleep(self.time_window)

        while True:
            start_time = end_time
            end_time = time.time()
            events = self.collect_data(query, start_time, end_time)
            self.send_to_dest_thru_hec(events)
            time.sleep(self.time_window)

def main():
    '''
    Main Function
    '''
    parser = argparse.ArgumentParser()
    parser.add_argument('--source_ip', default='', required=True,
                        help='source splunkd url')
    parser.add_argument('--dest_ip', default='', required=True,
                        help='destination splunk server with HEC port')
    parser.add_argument('--dest_token', default='', required=True,
                        help='HEC token used to post data to dest')
    parser.add_argument('--index_name', default='_internal', required=False,
                        help='splunk index name')
    parser.add_argument('--source_type', type=list, default=['*'], required=False,
                        help='List of source types')
    parser.add_argument('--time_window', type=int, default=5, required=False,
                        help='time window to run data collection in seconds')

    args = parser.parse_args()
    data_collector = ExportData(args.source_ip,
                                args.dest_ip,
                                args.dest_token,
                                args.index_name,
                                args.source_type,
                                args.time_window)
    data_collector.run()

if __name__ == '__main__':
    main()
