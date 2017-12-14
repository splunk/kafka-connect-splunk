#!/usr/bin/python

import logging
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
    def __init__(self, config):
        self.src = config.src
        self.dest = config.dest
        self.dest_token = config.dest_token
        self.index = config.index_name
        self.source_types = config.source_type
        self.source_admin_user = config.source_admin_user
        self.source_admin_password = config.source_admin_password
        self.timeout = config.timeout

    # pylint disable=no-self-use
    def _check_request_status(self, req_obj):
        '''
        check if a request is successful
        @param: req_obj
        returns True/False
        '''
        if not req_obj.ok:
            raise Exception('status code: {0} \n details: {1}'.format(
                str(req_obj.status_code), req_obj.text))

    def _check_source_connection(self):
        '''
        check if a source server connection is accessible
        returns True/False
        '''
        service_url = '{0}/services'.format(self.src)
        logger.info('requesting: %s', service_url)

        res = self._requests_retry_session().get(
            service_url,
            auth=(self.source_admin_user, self.source_admin_password),
            verify=False)
        self._check_request_status(res)
  

    def _check_dest_connection(self):
        '''
        check if a destination server connection is accessible by sending a test event
        returns True/False
        '''
        dest_url = '{0}/services/collector/event'.format(self.dest)
        logger.info('requesting: %s', dest_url)
        headers = {
            'Authorization': 'Splunk {token}'.format(token=self.dest_token),
            'Content-Type': 'application/json',
        }
        data = {
            'event': 'test',
        }

        res = self._requests_retry_session().post(dest_url, headers=headers,
                                                  data=json.dumps(data), verify=False)
        self._check_request_status(res)

    def _compose_search_query(self):
        '''
        compose a splunk search query with input index and source types
        returns job_str
        '''
        for idx, item in enumerate(self.source_types):
            self.source_types[idx] = 'sourcetype="{0}"'.format(item)

        source_type_str = ' OR '.join(self.source_types)
        job_str = 'search index="{index}" {source_type_search}' \
                   .format(index=self.index, source_type_search=source_type_str)

        logger.info('job_str: %s', job_str)

        return job_str

    def _collect_data(self, query, start_time, end_time):
        '''
        collect events from the source server
        @param: query (search query)
        @param: start_time (search start time)
        @param: end_time (search end time)
        returns events
        '''

        url = '{0}/services/search/jobs?output_mode=json'.format(self.src)
        logger.info('requesting: %s', url)
        data = {
            'search': query,
            'earliest_time': start_time,
            'latest_time': end_time,
        }

        create_job = self._requests_retry_session().post(
            url,
            auth=(self.source_admin_user, self.source_admin_password),
            verify=False, data=data)
        self._check_request_status(create_job)

        json_res = create_job.json()
        job_id = json_res['sid']
        events = self._wait_for_job_and__get_events(job_id)

        return events

    def _wait_for_job_and__get_events(self, job_id):
        '''
        wait for the search job to finish and collect the result events
        @param: job_id
        returns events
        '''
        events = []
        job_url = '{0}/services/search/jobs/{1}?output_mode=json'.format(self.src, str(job_id))
        logger.info('requesting: %s', job_url)

        for _ in range(self.timeout):
            res = self._requests_retry_session().get(
                job_url,
                auth=(self.source_admin_user, self.source_admin_password),
                verify=False)
            self._check_request_status(res)

            job_res = res.json()
            dispatch_state = job_res['entry'][0]['content']['dispatchState']

            if dispatch_state == 'DONE':
                events = self._get_events(job_id)
                break
            if dispatch_state == 'FAILED':
                raise Exception('Search job: {0} failed'.format(job_url))
            time.sleep(1)

        return events

    def _get_events(self, job_id):
        '''
        collect the result events from a search job
        @param: job_id
        returns events
        '''
        event_url = '{0}/services/search/jobs/{1}/events?output_mode=json'.format(self.src, str(job_id))
        logger.info('requesting: %s',  event_url)

        event_job = self._requests_retry_session().get(
            event_url, auth=(self.source_admin_user, self.source_admin_password), verify=False)
        self._check_request_status(event_job)

        event_job_json = event_job.json()
        events = event_job_json['results']

        return events

    def _transform_results_to_hec_events(self, events):
        '''
        transform the events collected from the source server to events that can be 
        accepted by hec event endpoint
        @param: events
        returns hec_events
        '''
        hec_events = []
        for event in events:
            temp = {}
            temp['event'] = event['_raw']
            parsed_t = time_parser.parse(event['_time'])
            temp['time'] = parsed_t.strftime('%s')
            temp['host'] = event['host']
            temp['source'] = event['source']
            temp['sourcetype'] = event['sourcetype']
            hec_events.append(temp)

        return hec_events

    def _send_to_dest_thru_hec(self, events):
        '''
        send collected events to the destination server
        @param: events
        '''
        if not events:
            logger.info('No events collected.')
            return

        hec_events = self._transform_results_to_hec_events(events)
        data = '\n'.join(json.dumps(event) for event in hec_events)
        headers = {
            'Authorization': 'Splunk {token}'.format(token=self.dest_token),
            'Content-Type': 'application/json',
        }

        dest_url = '{0}/services/collector/event'.format(self.dest)
        logger.info('sending data to : %s', dest_url)

        res = self._requests_retry_session().post(
            dest_url, verify=False, headers=headers, data=data)
        self._check_request_status(res)

    def _requests_retry_session(
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

    def _initialize_time_range(self, start_time, end_time, time_window):
        '''
        process the input start_time and end_time and validate the values
        @param: start_time
        @param: end_time
        @param: time_window
        returns [start_time, end_time]
        '''
        # if start_time is not specified, use current time
        if not start_time:
            start_time = int(time.time())
        if end_time and start_time >= end_time:
            raise Exception('start time should be less than end time')

        # sleep for the time window if end time is not specified
        # to make sure the data collection always have valid time range
        if end_time is None:
            time.sleep(time_window)

        return [start_time, end_time]

    def _compute_next_time_range(self, last_end_time, end_time, time_window):
        '''
        compute the next time range for data collection
        @param: last_end_time
        @param: end_time
        @oaram: time_window
        returns [next_start_time, next_end_time]
        '''
        next_start_time = last_end_time
        next_end_time = 0

        if end_time is None:
            time.sleep(time_window)
            next_end_time = last_end_time + time_window
        else:
            if  last_end_time + time_window < end_time:
                next_end_time = last_end_time + time_window
            else:
                next_end_time = end_time

        return [next_start_time, next_end_time]

    def run(self, start_time=None, end_time=None, time_window=5):
        '''
        function to run data collection and export
        @param: start_time
                start time in epoch seconds to run search job. If None, current time will be used
        @oaram: end_time
                end time in epoch seconds to run search job. If None, job will run forever
        @oaram: time_window
                time window in seconds to run search job. Default is 5 seconds
        '''
        [start_time, end_time] = self._initialize_time_range(start_time, end_time, time_window)

        query = self._compose_search_query()
        logger.info('Data collection (%s - %s) starts', start_time, end_time)

        self._check_source_connection()
        self._check_dest_connection()

        cur_start_time = start_time
        cur_end_time = start_time + time_window

        while cur_start_time < cur_end_time or end_time is None:
            logger.info('Collecting %s - %s', cur_start_time, cur_end_time)

            events = self._collect_data(query, cur_start_time, cur_end_time)
            self._send_to_dest_thru_hec(events)

            [cur_start_time, cur_end_time] = self._compute_next_time_range(
                cur_end_time, end_time, time_window)

        logger.info('Data collection is DONE')

def main():
    '''
    Main Function
    '''
    parser = argparse.ArgumentParser()
    parser.add_argument('--src', default='', required=True,
                        help='source splunkd url')
    parser.add_argument('--dest', default='', required=True,
                        help='destination splunk server with HEC port')
    parser.add_argument('--dest_token', default='', required=True,
                        help='HEC token used to post data to dest')
    parser.add_argument('--index_name', default='_internal', required=False,
                        help='splunk index name')
    parser.add_argument('--source_type', type=list, default=['*'], required=False,
                        help='List of source types')
    parser.add_argument('--source_admin_user', default='admin', required=False,
                        help='source splunk admin user')
    parser.add_argument('--source_admin_password', default='changed', required=False,
                        help='source splunk admin password')
    parser.add_argument('--timeout', type=int, default=30, required=False,
                        help='timeout for search job')

    args = parser.parse_args()
    data_collector = ExportData(args)
    data_collector.run()

if __name__ == '__main__':
    main()
