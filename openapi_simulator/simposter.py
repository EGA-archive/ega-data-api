#!/usr/bin/env python3
"""
Simpostor is a small wrapper around the imposter "scriptable, multipurpose mock
server" which can be used as a convenient way to list and run mock services from
OpenAPI specifications.
"""

import re
import os
import json
import logging
from subprocess import Popen, PIPE, check_output

import yaml

SPECIFICATIONS_DIR = 'openapi'

def color_print(text, color):
    """
    Simple wrapper around using ANSI codes to change text color.
    """
    colors = {'black':'0;30',
              'red':'0;31',
              'green':'0;32',
              'orange':'0;33',
              'blue':'0;34',
              'purple':'0;35',
              'cyan':'0;36',
              'gray':'0;37',
              'dark gray':'1;30',
              'light red':'1;31',
              'light green':'1;32',
              'yellow':'1;33',
              'light blue':'1;34',
              'light purple':'1;35',
              'light cyan':'1;36',
              'white':'1;37'}
    color = colors.get(color, '0')

    return '\033[' + color + 'm' + text + '\033[0m'

class ImposterWrapper():
    """
    Convenience wrapper for running mock services with imposter
    (https://github.com/outofcoffee/imposter/) from OpenAPI specifications.
    """

    def __init__(self, openapi_dir='openapi', image='outofcoffee/imposter-openapi'):
        self.min_docker_version = 18
        self.image = image
        self.run_checks()
        self.openapi_dir = openapi_dir
        self.available_services = self.find_services()
        self.running_services = self.find_running_services()

    def check_docker(self):
        """
        Checks that docker is available, and that the version is sufficient
        """
        try:
            version_string = check_output(['docker', '--version']).decode()
            version_string = version_string.strip()
        except FileNotFoundError:
            logging.error(("Error running docker from command line, is it "
                           'installed?'))
            return
        match = re.search(r"([0-9]+)\.([0-9]+)\.([0-9]+)", version_string)
        if match:
            major = int(match.group(1))
            if major < self.min_docker_version:
                logging.warning(('Docker version %s or higher is recommended '
                                 '(you are running %s).'),
                                self.min_docker_version, version_string)
                return
        else:
            logging.warning(('Could not parse docker version string. Things '
                             'might still work but it is not a good sign.'))
            return
        logging.debug('docker looks good (version %s)', version_string)

    def check_docker_image(self):
        """
        Checks if the target docker image (stored in self.image) is available to
        the system, and installs it otherwise.
        """
        available = check_output(['docker', 'images', self.image, '-q'])
        if not available:
            logging.info(('The %s docker image is not available, attempting '
                          'download'), self.image)
            pull_result = check_output(['docker', 'pull', self.image])
            available = check_output(['docker', 'images', self.image, '-q'])
            if not available:
                logging.error(('Something went wrong pulling the image. '
                               'The command returned: %s'), pull_result)
                return
            logging.info('Successfully downloaded image.')
        logging.debug('The %s docker image is available', self.image)

    def find_running_services(self):
        """
        Looks for running docker services.
        """
        services = {}
        for service, spec in self.available_services.items():
            service_status = self.service_status(service)
            if service_status['running']:
                spec['docker_id'] = service_status['id']
                services[service_status['port']] = spec
        return services

    def find_services(self):
        """
        Returns a list of all service specifications available in the
        self.openapi directory.

        A specification needs to be a directory with a .yml or .yaml file, as
        well as a json file, where the yaml file and can be parsed using PyYAML,
        and the json file can be parsed by json.
        """
        specs = {}
        for subdir in os.listdir(self.openapi_dir):
            if not os.path.isdir(os.path.join(self.openapi_dir, subdir)):
                continue
            service = {'yaml':None, 'json':None, 'name':subdir,
                       'path':os.path.join(self.openapi_dir, subdir),
                       'port':8443, 'label':None}
            for filename in os.listdir(service['path']):
                file_path = os.path.join(service['path'], filename)
                ext = os.path.splitext(filename)[-1].lower()
                logging.debug("Parsing file %s", file_path)
                # check if we have a valid yaml-file
                if ext in ['.yml', '.yaml']:
                    try:
                        data = yaml.safe_load(open(file_path))
                        # while we have the file open - check for port
                        try:
                            service['port'] = \
                                int(data.get('host', '').split(':')[-1])
                        except ValueError:
                            logging.debug("Couldn't parse port")
                        # also set the label and yaml filename
                        service['label'] = '{name}-{port}'.format(**service)
                        service['yaml'] = filename
                    except yaml.scanner.ScannerError:
                        logging.warning(("file '%s' looks like a specification "
                                         "but can't be parsed by PyYAML.",
                                         filename))
                # check if we have a valid json-file
                if ext in ['.json']:
                    try:
                        json.load(open(file_path))
                        service['json'] = filename
                    except json.decoder.JSONDecodeError:
                        logging.warning(("file '%s' looks like a config file "
                                         "but can't be parsed.", filename))
            if service['yaml'] and service['json']:
                specs[service['name']] = service
        return specs

    def service_status(self, name):
        """
        Checks the current status of the named service.
        """
        if name not in self.available_services:
            logging.error('Unknown service %s', name)
        spec = self.available_services[name]
        docker_cmd = ['docker', 'ps', '--format', '{{.ID}}\t{{.Ports}}', '--filter']
        output = check_output(docker_cmd + ['label='+spec['label']]).decode()
        service_status = {'running':False, 'port':None}
        if output:
            service_status['running'] = True
            service_status['id'] = output.split('\t')[0]
            port_match = re.search(r'->([0-9]+)', output)
            if port_match:
                service_status['port'] = int(port_match.group(1))

        return service_status

    def start_service(self, name):
        """
        Starts one of the available services.
        """
        # check if the service is available
        if name not in self.available_services:
            logging.error('Service "%s" is not available.')
            return

        # check if the service is already running
        service = self.available_services[name]
        for running in self.running_services.values():
            if running['name'] == name:
                logging.info("Service '%s' is already running", running['name'])
                return

        # figure out yaml path
        path = os.path.join(service['path'], service['yaml'])
        logging.debug('Using yaml file path %s', path)

        # find the correct (or default, or an available) port
        port = 8443
        with open(path) as spec_file:
            yaml_spec = yaml.safe_load(spec_file)
            logging.debug('Getting service port from specification.')
            try:
                port = int(yaml_spec.get('host', '').split(':')[-1])
            except ValueError:
                logging.warning(('Could not get port from specification, using '
                                 'default value of %s', port))
        while port in self.running_services:
            other = self.running_services[port]['name']
            logging.warning(('Service %s is set to run on port %s, but port is '
                             'used by %s. Trying %s.'), service['name'], port,
                            other, port+1)
            port += 1

        # start the service
        logging.info('Starting %s on port %s', service['name'], port)
        logging.debug('Using label: %s_%s', service['name'], port)
        command = ['docker', 'run', '-p', '{port}:{port}'.format(port=port),
                   '--label', service['label'],
                   '-v',
                   '%s:/opt/imposter/config' % os.path.abspath(service['path']),
                   'outofcoffee/imposter-openapi',
                   '--plugin',
                   'com.gatehill.imposter.plugin.openapi.OpenApiPluginImpl',
                   '--configDir', '/opt/imposter/config',
                   '--listenPort', str(port)]

        logging.debug("Running command %s", " ".join(command))
        process = Popen(command, stdout=PIPE, stderr=PIPE)
        self.running_services[port] = {'name':service['name'], 'cmd':command,
                                       'proc':process}

    def stop_service(self, name):
        """
        Stops a running service.
        """
        if name == 'all':
            for service in self.available_services:
                self.stop_service(service)
            return

        if name not in self.available_services:
            logging.warning("Can't stop unknown service '%s'", name)
            return

        for service in self.running_services.values():
            if service['name'].lower() == name.lower():
                logging.info("Stopping service %s", service['name'])
                check_output(['docker', 'stop', service['docker_id']])


    def run_checks(self):
        """
        Runs all self-checks to see if there are any environment problems which
        could hinder the system from running.
        """
        self.check_docker()
        self.check_docker_image()


if __name__ == '__main__':

    import argparse

    PARSER = argparse.ArgumentParser(description=__doc__)
    # Positional arguments
    PARSER.add_argument('services', nargs='*', default=[],
                        help="One or more OpenAPI service names, or 'all'.")

    # Optional arguments
    PARSER.add_argument('-l', '--list', action='store_true',
                        help='list all services available for simulation.')
    PARSER.add_argument('-o', '--omit', nargs='+', default=[],
                        help='Set services to not be simulated.')
    PARSER.add_argument('-s', '--stop', nargs='+', default=[],
                        help="Set service names to stop, or 'all'.")
    PARSER.add_argument('--service_dir', default='openapi',
                        help='Set service directory for openapi specifications')

    # Logging arguments
    PARSER.add_argument('-v', '--verbose', action='count', default=3,
                        help='Increase output Verbosity.')
    PARSER.add_argument('-q', '--quiet', action='count', default=0,
                        help='Decrease output Verbosity.')

    ARGS = PARSER.parse_args()

    if ARGS.services == [] and not ARGS.list and not ARGS.stop:
        PARSER.print_help()
        print("Available services for simulation:")
        ARGS.list = True

    logging.basicConfig(format='%(asctime)s %(levelname)s: %(message)s',
                        datefmt='%H:%M:%S',
                        level=(5-ARGS.verbose+ARGS.quiet)*10)

    IMPOSTER = ImposterWrapper(ARGS.service_dir)

    # list available specifications (if asked to)
    if ARGS.list:
        if not IMPOSTER.available_services:
            print('No valid service specifications found.')
        else:
            LENGTH = max([len(s) for s in IMPOSTER.available_services])
            for service_name in IMPOSTER.available_services:
                SERVICE_STATUS = IMPOSTER.service_status(service_name)
                COLOR = 'red'
                STATUS = 'Not running'
                PORT = ''
                if SERVICE_STATUS['running']:
                    STATUS = 'Running'
                    COLOR = 'green'
                    PORT = SERVICE_STATUS['port']
                STATUS = '[%s]' % color_print(STATUS, COLOR)
                if PORT:
                    STATUS += ' (port %s)' % PORT
                print('{0:{length}} {1}'.format(service_name, STATUS,
                                                length=LENGTH))

    # figure out what services to start
    SERVICES = []
    for service_name in ARGS.services:
        # add all specs if 'all' is set
        if service_name == 'all':
            SERVICES = list(IMPOSTER.available_services.keys())
        # make sure the spec is in 'all' before adding it
        elif service_name in IMPOSTER.available_services.keys():
            SERVICES += [service_name]
        # else, print a warning that the spec is unknown
        else:
            logging.warning('Unknown service: %s', service_name)

    # remove services in --omit
    for service_name in ARGS.omit:
        if service_name not in IMPOSTER.available_services.keys():
            logging.warning("Can't remove unknown service: %s", service_name)
            continue
        if service_name in SERVICES:
            logging.debug("removing omitted service: %s", service_name)
            del SERVICES[service_name]

    for service_name in SERVICES:
        IMPOSTER.start_service(service_name)

    for service_name in ARGS.stop:
        IMPOSTER.stop_service(service_name)
