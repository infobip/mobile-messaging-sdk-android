#!/bin/bash

UPLOADER_VERSION=2.12
# Put your TestFairy API_KEY here. Find it in your TestFairy account settings.
TESTFAIRY_API_KEY=$4

# Tester Groups that will be notified when the app is ready. Setup groups in your TestFairy account testers page.
# This parameter is optional, leave empty if not required
TESTER_GROUPS=ANDROID_NOTIFICATION,ANDROID_RELEASE_NOTIFICATION

# Should email testers about new version. Set to "off" to disable email notifications.
NOTIFY="on"

# If AUTO_UPDATE is "on" all users will be prompt to update to this build next time they run the app
AUTO_UPDATE="off"

# The maximum recording duration for every test.
MAX_DURATION="1m"

# Is video recording enabled for this build. valid values:  "on", "off", "wifi"
VIDEO="off"

# Comment text will be included in the email sent to testers
COMMENT="New build available!"

# locations of various tools
CURL=curl

SERVER_ENDPOINT=https://upload.testfairy.com

usage() {
	echo "Usage: testfairy-upload-android.sh APP_FILENAME TESTER_GROUPS MESSAGE API_KEY"
	echo
}

verify_tools() {

	# Check 'curl' tool
	${CURL} --help >/dev/null
	if [ $? -ne 0 ]; then
		echo "Could not run curl tool, please check settings"
		exit 1
	exit 1
    fi
}

verify_settings() {
	if [ -z "${TESTFAIRY_API_KEY}" ]; then
		usage
		echo "Please update API_KEY with your private API key, as noted in the Settings page"
		exit 1
	fi
}

if [ $# -ne 3 ]; then
	usage
	exit 1
fi

# before even going on, make sure all tools work
verify_tools
verify_settings

APK_FILENAME=$1
if [ ! -f "${APK_FILENAME}" ]; then
	usage
	echo "Can't find file: ${APK_FILENAME}"
	exit 2
fi

TESTER_GROUPS=$2
COMMENT=$3

# temporary file paths
DATE=`date`

/bin/echo -n "Uploading ${APK_FILENAME} to TestFairy.. "
JSON=$( ${CURL} -s ${SERVER_ENDPOINT}/api/upload -F api_key=${TESTFAIRY_API_KEY} -F file="@${APK_FILENAME}" -F video="${VIDEO}" -F max-duration="${MAX_DURATION}" -F comment="${COMMENT}" -F testers-groups="${TESTER_GROUPS}" -F auto-update="${AUTO_UPDATE}" -F notify="${NOTIFY}" )

URL=$( echo ${JSON} | sed 's/\\\//\//g' | sed -n 's/.*"build_url"\s*:\s*"\([^"]*\)".*/\1/p' )
if [ -z "$URL" ]; then
	echo "FAILED!"
	echo
	echo "Build uploaded, but no reply from server. Please contact support@testfairy.com"
	exit 1
fi

echo "OK!"
echo
echo "Build was successfully uploaded to TestFairy and is available at:"
echo ${URL}
echo ""
echo "Please visit this link, go to SETTINGS/INSIGHTS and disable RECORDING"
echo ""