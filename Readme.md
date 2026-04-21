# Generare il jar
mvn clean package

# Eseguire l'applicazione
java -jar target\gaikingCopilot-0.0.1-SNAPSHOT.jar

# Per avere il token per github copilot via GitHub OAuth
1. fai partire l’app
2. apri nel browser http://localhost:8080/api/copilot/auth/login
3. completa login e consenso GitHub
4. quando GitHub torna sul callback, il tuo backend esegue exchangeAuthorizationCodeForAccessToken(...)

Aprire la devtool del browser, andare in Applicazione -> Store -> Cookies -> http://localhost:8080 
recuperare il valore di SESSION e inserirlo nella curl: 
curl.exe --cookie "SESSION=c05f0f29-9769-42dd-a4e9-9b962f5646a6" "http://localhost:8080/getCopilotModels"

# Run api newProject/mvn_selenium_junit5_cucumber
# con bash
curl --request GET \
  --url 'http://localhost:8080/newProject/mvn_selenium_junit5_cucumber?model=gpt-5.3-codex&projectName=ta_copilot&groupId=com.nttdata&javaVersion=25&seleniumVersion=4.41.0&junitVersion=5.13.4&junitPlatformVersion=1.13.4&cucumberVersion=7.27.2&webdrivermanagerVersion=6.3.3&surefireVersion=3.2.5&compilerPluginVersion=3.13.0' \
  --cookie 'SESSION=4eca149d-441b-4389-b373-5b9f947368aa'

# powershell
curl.exe --request GET `
  --url "http://localhost:8080/newProject/mvn_selenium_junit5_cucumber?model=gpt-5.3-codex&projectName=ta_copilot&groupId=com.nttdata&javaVersion=25&seleniumVersion=4.41.0&junitVersion=5.13.4&junitPlatformVersion=1.13.4&cucumberVersion=7.27.2&webdrivermanagerVersion=6.3.3&surefireVersion=3.2.5&compilerPluginVersion=3.13.0" `
  --cookie "SESSION=4eca149d-441b-4389-b373-5b9f947368aa"

  # Run api per il logout
  #bash
  curl --request POST --cookie "SESSION=c05f0f29-9769-42dd-a4e9-9b962f5646a6" "http://localhost:8080/api/copilot/auth/logout"
  # powershell
  curl.exe --request POST --cookie "SESSION=c05f0f29-9769-42dd-a4e9-9b962f5646a6" "http://localhost:8080/api/copilot/auth/logout" 
