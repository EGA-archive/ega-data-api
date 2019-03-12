#
# Wrapper bash script around the OpenAPI simulator.
#
# This script makes sure that there's a python environment with the correct
# packages for the simulator, and then call the simulator.
#
# Note that this script has been silenced, so to debug any problems, you will
# need to remove the redirects from the virtualenv and pip3 commands.

SIMULATOR="openapi_simulator"
VIRTUALENV="venv_simulator"

# Change directory to the script directory
cd $(dirname $0)

if [[ ! -d "$VIRTUALENV" ]]
then
    echo "[Creating virtualenv '$VIRTUALENV' for the simulator]"
    virtualenv -p python3 "$VIRTUALENV" >/dev/null 2>&1
fi

. $VIRTUALENV/bin/activate
pip3 install -r $SIMULATOR/requirements.txt >/dev/null 2>&1

./$SIMULATOR/simposter.py $@
