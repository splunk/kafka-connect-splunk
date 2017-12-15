#!/usr/bin/python

import logging
import argparse
import time
import json
import dateutil.parser as time_parser
import collections

import requests
from requests.packages.urllib3.util.retry import Retry
from requests.adapters import HTTPAdapter

logger = logging.getLogger('export_data')

ExportParams = collections.namedtuple(
    'ExportParams',
    ['src_splunk_uri', 'src_splunk_user', 'src_splunk_password',
     'dest_splunk_hec', 'dest_hec_token',
     'src_index', 'src_sourcetypes',
     'timeout'])


class ExportData(object):
    ''' Export Data class
    This class handles real time data collection from the source splunk server
    with given index and sourcetype and export the events to
    destination splunk server
    '''
    def __init__(self, config):
        self.src_splunk_uri = config.src_splunk_uri
        self.src_splunk_user = config.src_splunk_user
        self.src_splunk_password = config.src_splunk_password

        self.src_index = config.src_index
        self.src_source_types = config.src_sourcetypes

        self.dest_splunk_hec = config.dest_splunk_hec
        self.dest_hec_token = config.dest_hec_token

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
        Check if a source server connection is accessible
        returns True/False
        '''
        service_url = '{0}/services'.format(self.src_splunk_uri)
        logger.info('requesting: %s', service_url)

        res = self._requests_retry_session().get(
            service_url,
            auth=(self.src_splunk_user, self.src_splunk_password),
            verify=False)
        self._check_request_status(res)

    def _check_dest_splunk_hec_connection(self):
        '''
        Check if a dest_splunk_hecination server connection is accessible by
        sending a test event returns True/False
        '''
        dest_splunk_hec_url = '{0}/services/collector/event'.format(
            self.dest_splunk_hec)
        logger.info('requesting: %s', dest_splunk_hec_url)
        headers = {
            'Authorization': 'Splunk {token}'.format(token=self.dest_hec_token),
            'Content-Type': 'application/json',
        }
        data = {
            'event': 'test',
        }

        res = self._requests_retry_session().post(
            dest_splunk_hec_url, headers=headers, data=json.dumps(data),
            verify=False)
        self._check_request_status(res)

    def _compose_search_query(self):
        '''
        Compose a splunk search query with input index and source types
        returns job_str
        '''
        for idx, item in enumerate(self.src_source_types):
            self.src_source_types[idx] = 'sourcetype="{0}"'.format(item)

        source_type_str = ' OR '.join(self.src_source_types)
        job_str = 'search index="{index}" {source_type_search}'.format(
            index=self.src_index, source_type_search=source_type_str)

        logger.info('job_str: %s', job_str)

        return job_str

    def _collect_data(self, query, start_time, end_time):
        '''
        Collect events from the source server
        @param: query (search query)
        @param: start_time (search start time)
        @param: end_time (search end time)
        returns events
        '''

        url = '{0}/services/search/jobs?output_mode=json'.format(
            self.src_splunk_uri)
        logger.info('requesting: %s', url)
        data = {
            'search': query,
            'earliest_time': start_time,
            'latest_time': end_time,
        }

        create_job = self._requests_retry_session().post(
            url,
            auth=(self.src_splunk_user, self.src_splunk_password),
            verify=False, data=data)
        self._check_request_status(create_job)

        json_res = create_job.json()
        job_id = json_res['sid']
        events = self._wait_for_job_and__get_events(job_id)

        return events

    def _wait_for_job_and__get_events(self, job_id):
        '''
        Wait for the search job to finish and collect the result events
        @param: job_id
        returns events
        '''
        events = []
        job_url = '{0}/services/search/jobs/{1}?output_mode=json'.format(
            self.src_splunk_uri, str(job_id))
        logger.info('requesting: %s', job_url)

        for _ in range(self.timeout):
            res = self._requests_retry_session().get(
                job_url,
                auth=(self.src_splunk_user, self.src_splunk_password),
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
        event_url = '{0}/services/search/jobs/{1}/events?output_mode=json'.format(
            self.src_splunk_uri, str(job_id))
        logger.info('requesting: %s', event_url)

        event_job = self._requests_retry_session().get(
            event_url, auth=(self.src_splunk_user, self.src_splunk_password),
            verify=False)
        self._check_request_status(event_job)

        event_job_json = event_job.json()
        events = event_job_json['results']

        return events

    def _transform_results_to_hec_events(self, events):
        '''
        transform the events collected from the source server to events that
        can be accepted by hec event endpoint
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

    def _send_to_dest_splunk_hec_thru_hec(self, events):
        '''
        send collected events to the dest_splunk_hecination server
        @param: events
        '''
        if not events:
            logger.info('No events collected.')
            return

        hec_events = self._transform_results_to_hec_events(events)
        data = '\n'.join(json.dumps(event) for event in hec_events)
        headers = {
            'Authorization': 'Splunk {token}'.format(
                token=self.dest_hec_token),
            'Content-Type': 'application/json',
        }

        dest_splunk_hec_url = '{0}/services/collector/event'.format(
            self.dest_splunk_hec)
        logger.info('sending %d events to : %s',
                    len(events), dest_splunk_hec_url)

        res = self._requests_retry_session().post(
            dest_splunk_hec_url, verify=False, headers=headers, data=data)
        self._check_request_status(res)

    def _requests_retry_session(
            self,
            retries=10,
            backoff_factor=0.1,
            status_forcelist=(500, 502, 504)):
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
            start_time = time.time()

        if end_time and start_time >= end_time:
            raise Exception('start time should be less than end time')

        # sleep for the time window if end time is not specified
        # to make sure the data collection always have valid time range
        if end_time is None:
            from_start = time.time() - start_time
            if from_start < time_window:
                time.sleep(time_window - from_start)

        return start_time, end_time

    def _compute_next_time_range(self, last_end_time, end_time, time_window):
        '''
        compute the next time range for data collection
        @param: last_end_time
        @param: end_time
        @oaram: time_window
        returns next_start_time, next_end_time
        '''
        if end_time is None:
            now = time.time()
            if last_end_time + time_window > now:
                time.sleep(last_end_time + time_window - now)
            next_end_time = last_end_time + time_window
        else:
            if last_end_time + time_window < end_time:
                next_end_time = last_end_time + time_window
            else:
                next_end_time = end_time

        return last_end_time, next_end_time

    def export(self, query, start_time, end_time):
        events = self._collect_data(query, start_time, end_time)
        self._send_to_dest_splunk_hec_thru_hec(events)

    def run(self, start_time=None, end_time=None, time_window=300):
        '''
        function to run data collection and export
        @param: start_time
                start time in epoch seconds to run search job. If None, current
                time will be used
        @oaram: end_time
                end time in epoch seconds to run search job. If None,
                job will run forever
        @oaram: time_window
                time window in seconds to run search job. Default is 5 seconds
        '''
        self._check_source_connection()
        self._check_dest_splunk_hec_connection()

        start_time, end_time = self._initialize_time_range(
            start_time, end_time, time_window)

        query = self._compose_search_query()
        logger.info('Data collection (%s - %s) starts', start_time, end_time)

        cur_start_time = start_time
        cur_end_time = start_time + time_window

        while cur_start_time < cur_end_time or end_time is None:
            logger.info('Collecting %s - %s', cur_start_time, cur_end_time)

            self.export(query, cur_start_time, cur_end_time)

            cur_start_time, cur_end_time = self._compute_next_time_range(
                cur_end_time, end_time, time_window)

        logger.info('Data collection is DONE')


def main():
    '''
    Main Function
    '''
    parser = argparse.ArgumentParser()
    parser.add_argument('--src_splunk_uri', default='', required=True,
                        help='Source splunkd url. For example, https://localhost:8089')
    parser.add_argument('--src_splunk_user', default='admin', required=False,
                        help='Source splunk user')
    parser.add_argument('--src_splunk_password', default='changed', required=False,
                        help='Source splunk password for user')
    parser.add_argument('--src_index', default='_internal', required=False,
                        help='Splunk index name to query from')
    parser.add_argument('--src_sourcetypes', type=list, default=['*'], required=False,
                        help='List of sourcetypes to query from. For example ["metric", "perf"]')

    parser.add_argument('--dest_splunk_hec', default='', required=True,
                        help='Destination splunk server with HEC port. For example, https://localhost:8088')
    parser.add_argument('--dest_hec_token', default='', required=True,
                        help='HEC token for destination splunk used to post data to dest_splunk_hec Splunk HEC')
    parser.add_argument('--timeout', type=int, default=300, required=False,
                        help='timeout for search job')

    args = parser.parse_args()
    data_collector = ExportData(args)
    data_collector.run(start_time=time.time() - 1*86400)


if __name__ == '__main__':
    main()
