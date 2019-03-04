#!/usr/bin/env python3
"""
This is a small wrapper around the imposter "scriptable, multipurpose mock
server" which can be used as a convenient way to list and run mock services from
OpenAPI specifications.
"""

import re
import os
import sys
import json
import logging
import argparse
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

    def restart_service(self, name):
        """
        Restarts a running service.
        """
        self.stop_service(name)
        self.start_service(name)

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
                                 'default value of %s'), port)
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

        for port, service in self.running_services.items():
            if service['name'].lower() == name.lower():
                logging.info("Stopping service %s", service['name'])
                check_output(['docker', 'stop', service['docker_id']])
                del self.running_services[port]
                break

    def run_checks(self):
        """
        Runs all self-checks to see if there are any environment problems which
        could hinder the system from running.
        """
        self.check_docker()
        self.check_docker_image()

class MultilevelParser():
    """
    Multilevel argument parser, where a subcommand is used to control which part
    of the parser is used.
    """

    def __init__(self):
        parser = argparse.ArgumentParser(
            description='Mock service provider using imposter.',
            usage="""./simulator <command> [<args>]

The available subcommands for the simulator are:
   start      starts one or more named services, or 'all', default: 'all'.
   stop       stops one or more named services, or 'all', default: 'all'.
   restart    restarts one or more named services, or 'all', default: 'all'.
   list       lists all available services.
""")
        parser.add_argument('command', help='Subcommand to run')
        # parse_args defaults to [1:] for args, but you need to
        # exclude the rest of the args too, or validation will fail
        args = parser.parse_args(sys.argv[1:2])
        if not hasattr(self, "subcommand_%s" % args.command):
            logging.error('Unrecognized command, "%s"', args.command)
            parser.print_help()
            exit(1)

        # remove the first subcommand from sys.argv
        del sys.argv[1]
        # use call the function named as subcommand
        getattr(self, "subcommand_%s" % args.command)()

    @staticmethod
    def subcommand_start():
        """
        Used to start mock services.
        """
        parser = argparse.ArgumentParser(
            description='Starts one or more services')

        parser.add_argument('services', nargs='*', default=['all'])

        parser.add_argument('--service_dir', default='openapi',
                            help=('Set service directory for openapi '
                                  'specifications'))

        args = parser.parse_args()
        imposter = ImposterWrapper(args.service_dir)

        if 'all' in args.services:
            args.services.remove('all')
            args.services += list(imposter.available_services.keys())

        for service_name in list(set(args.services)):
            imposter.start_service(service_name)

    @staticmethod
    def subcommand_stop():
        """
        Used to stop mock services.
        """
        parser = argparse.ArgumentParser(
            description='Stops one or more services')

        parser.add_argument('services', nargs='*', default=['all'])

        parser.add_argument('--service_dir', default='openapi',
                            help=('Set service directory for openapi '
                                  'specifications'))

        args = parser.parse_args()
        imposter = ImposterWrapper(args.service_dir)

        if 'all' in args.services:
            args.services.remove('all')
            args.services += list(imposter.available_services.keys())

        for service_name in list(set(args.services)):
            imposter.stop_service(service_name)

    @staticmethod
    def subcommand_restart():
        """
        Used to restart mock services.
        """
        parser = argparse.ArgumentParser(
            description='Restarts one or more services')

        parser.add_argument('--service_dir', default='openapi',
                            help=('Set service directory for openapi '
                                  'specifications'))

        parser.add_argument('services', nargs='*', default=['all'])

        args = parser.parse_args()
        imposter = ImposterWrapper(args.service_dir)

        running = [s['name'] for s in imposter.running_services.values()]

        if 'all' in args.services:
            args.services.remove('all')
            args.services += running

        if not args.services:
            print("No running services to restart")

        for service_name in list(set(args.services)):
            imposter.restart_service(service_name)

    @staticmethod
    def subcommand_list():
        """
        Used to list available mock services.
        """
        parser = argparse.ArgumentParser(
            description='Lists all available mock services')

        parser.add_argument('--service_dir', default='openapi',
                            help=('Set service directory for openapi '
                                  'specifications'))

        args = parser.parse_args()
        imposter = ImposterWrapper(args.service_dir)

        if not imposter.available_services:
            print('No valid service specifications found.')
        else:
            length = max([len(s) for s in imposter.available_services])
            for service_name in imposter.available_services:
                service_status = imposter.service_status(service_name)
                color = 'red'
                status = 'Not running'
                port = ''
                if service_status['running']:
                    status = 'Running'
                    color = 'green'
                    port = service_status['port']
                status = '[%s]' % color_print(status, color)
                if port:
                    status += ' (port %s)' % port
                print('{0:{length}} {1}'.format(service_name, status,
                                                length=length))

if __name__ == '__main__':

    logging.basicConfig(format='%(asctime)s %(levelname)s: %(message)s',
                        datefmt='%H:%M:%S',
                        level=logging.INFO)

    MultilevelParser()
