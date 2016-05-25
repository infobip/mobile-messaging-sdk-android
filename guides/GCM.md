### Configuring your application for Push Notifications over Google Cloud Messaging

Google Cloud Messaging is a service which allows Infobip to send Push Messages from the Infobip server to your users’ Android-powered devices. You will need to create and configure project at Google Developers site in order to enable Push Notifications for your applications.

1. Go to [Google Developers](https://developers.google.com/mobile/add) and start creating your application project. First you will need to choose a platform for push configuration.
<center><img src="images/GoogleDevelopersPickPlatform.png?raw=true" alt="Start picking platform"/></center>

2. Choose "Enable services for my Android App".
<center><img src="images/GoogleDevelopersPickAndroid.png?raw=true" alt="Pick Android platform"/></center>

3. On the next screen you will need to choose a new name for your project and also provide Android package name of your Android application. You can also change default settings such as sharing you data with Google and your country/region. After your're done with settings, just press "Choose and configure services".
<center><img src="images/GoogleDevelopersConfigureApp.png?raw=true" alt="Configure project"/></center>

4. You will need to pick Cloud Messaging service from the list of services available for your application.
<center><img src="images/GoogleDevelopersEnableGCM.png?raw=true" alt="Pick Cloud Messaging"/></center>

5. Then just press "Enable Google Cloud Messaging".
<center><img src="images/GoogleDevelopersEnableGCMSetting.png?raw=true" alt="Enable Cloud Messaging"/></center>

6. After that you will have your Server API Key and Sender ID.
<center><img src="images/GoogleDevelopersDone.png?raw=true" alt="Google Cloud Messaging credentials"/></center>

### Obtaining Google Cloud Messaging credentials for an existing project

If you already have your project set up on google developers site, you can just follow the same steps and pick existing project name and Android package name. Then you will see list of services for your project with Cloud Messaging enabled. You will need to press "Generate configuration files" on that screen.
<center><img src="images/GoogleDevelopersGenerateConfigurationFiles.png?raw=true" alt="Generate configuration files"/></center>

Your Google Cloud Messaging Server API Key and Sender ID will be available on the next screen.
<center><img src="images/GoogleDevelopersGCMSettings.png?raw=true" alt="Google Cloud Messaging credentials"/></center>