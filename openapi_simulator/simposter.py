#!/usr/bin/env python3
"""
Simpostor is a small wrapper around the imposter "scriptable, multipurpose mock
server" which can be used as a convenient way to list and run mock services from
OpenAPI specifications.
"""

import os
import json
import logging
from copy import deepcopy
from subprocess import Popen, PIPE, check_output

import yaml

SPECIFICATIONS_DIR = 'openapi'
IMPOSTERS = {}

def list_services(spec_dir):
    """
    Lists all service specifications available in the spec_dir directory.

    A specification needs to be a directory with a .yml or .yaml file, as well
    as a json file, where the yaml file and can be parsed using PyYAML, and the
    json file can be parsed by json.
    """
    specs = {}
    for subdir in os.listdir(spec_dir):
        if not os.path.isdir(os.path.join(spec_dir, subdir)):
            continue
        service = {'yaml':None, 'json':None, 'name':subdir,
                   'path':os.path.join(spec_dir, subdir),
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
                    logging.warning(("file '%s' looks like a specification but "
                                     "can't be parsed by PyYAML.", filename))
            # check if we have a valid json-file
            if ext in ['.json']:
                try:
                    json.load(open(file_path))
                    service['json'] = filename
                except json.decoder.JSONDecodeError:
                    logging.warning(("file '%s' looks like a config file but "
                                     "can't be parsed.", filename))
        if service['yaml'] and service['json']:
            specs[service['name']] = service
    return specs

def start_imposter(service):
    """
    Starts an imposter instance from a service specification.
    """
    path = os.path.join(service['path'], service['yaml'])
    logging.debug('Using yaml file path %s', path)

    port = 8443
    with open(path) as spec_file:
        yaml_spec = yaml.safe_load(spec_file)
        try:
            port = int(yaml_spec.get('host', '').split(':')[-1])
        except ValueError:
            logging.warning(('Could not get port from specification, using '
                             'default value of %s', port))
    while port in IMPOSTERS:
        other = IMPOSTERS[port]['name']
        logging.warning(('Service %s is set to run on port %s, but port is '
                         'used by %s. Trying %s.'), service['name'], port,
                        other, port+1)
        port += 1
    logging.info('Starting %s on port %s', service['name'], port)
    logging.debug('Using label: %s_%s', service['name'], port)
    command = ['docker', 'run', '-p', '{port}:{port}'.format(port=port),
               '--label', service['label'], '-v',
               '%s:/opt/imposter/config' % os.path.abspath(service['path']),
               'outofcoffee/imposter-openapi', '--plugin',
               'com.gatehill.imposter.plugin.openapi.OpenApiPluginImpl',
               '--configDir', '/opt/imposter/config', '--listenPort', str(port)]

    process = Popen(command, stdout=PIPE, stderr=PIPE)
    IMPOSTERS[port] = {'name':service['name'], 'cmd':command,
                       'proc':process}

if __name__ == '__main__':

    import argparse

    PARSER = argparse.ArgumentParser(description=__doc__)
    # Positional arguments
    PARSER.add_argument('specifications', nargs='*', default=[],
                        help=('One or more OpenAPI service specifications, or '
                              "'all'."))

    # Optional arguments
    PARSER.add_argument('-l', '--list', action='store_true',
                        help='list all services available for simulation.')
    PARSER.add_argument('-o', '--omit', nargs='+', default=[],
                        help='Set specifications to not be simulated.')
    PARSER.add_argument('-s', '--schema',
                        default='openapi_simulator/schema.yaml',
                        help='OpenAPI schema to use for validation.')

    # Logging arguments
    PARSER.add_argument('-v', '--verbose', action='count', default=3,
                        help='Increase output Verbosity.')
    PARSER.add_argument('-q', '--quiet', action='count', default=0,
                        help='Decrease output Verbosity.')

    ARGS = PARSER.parse_args()

    logging.basicConfig(format='%(asctime)s %(levelname)s: %(message)s',
                        datefmt='%H:%M:%S',
                        level=(5-ARGS.verbose+ARGS.quiet)*10)

    # list available specifications (if asked to)
    ALL_SPECS = list_services(SPECIFICATIONS_DIR)
    if ARGS.list:
        if not ALL_SPECS:
            print('No valid specifications found.')
        else:
            DOCKER_CMD = ['docker', 'ps', '-q', '--filter']
            for spec in ALL_SPECS.values():
                label = 'label={}'.format(spec['label'])
                docker_id = check_output(DOCKER_CMD + [label]).strip()
                if docker_id:
                    running = '[\033[0;32mrunning\033[0m]'
                else:
                    running = ''
                print(' - {0:{length}} {1}'.format(spec['name'], running,
                                                   length=12))

    # format specifications list to be proper filenames
    SPECS = {}
    for spec in ARGS.specifications:
        # add all specs if 'all' is set
        if spec == 'all':
            SPECS = deepcopy(ALL_SPECS)
        # make sure the spec is in 'all' before adding it
        elif spec in ALL_SPECS:
            SPECS[spec] = ALL_SPECS[spec]
        # else, print a warning that the spec is unknown
        else:
            logging.warning('Unknown specification: %s', spec)

    # remove specs in --omit
    for spec in ARGS.omit:
        if spec not in ALL_SPECS:
            logging.warning("Can't remove unknown specification: %s", spec)
            continue
        if spec in SPECS:
            logging.debug("removing omitted spec: %s", spec)
            del SPECS[spec]

    for spec in SPECS.values():
        start_imposter(spec)
