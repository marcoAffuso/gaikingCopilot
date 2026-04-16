# Generare il jar
mvn clean package

# Eseguire l'applicazione
java -jar target\gaikingCopilot-0.0.1-SNAPSHOT.jar

# Run api newProject/mvn_selenium_junit5_cucumber
# con bash
curl --request GET \
  --url "http://localhost:8080/newProject/mvn_selenium_junit5_cucumber?model=gpt-5.3-codex&projectName=ta_copilot&javaVersion=25&seleniumVersion=4.41.0&junitVersion=5.13.4&junitPlatformVersion=1.13.4&cucumberVersion=7.27.2&webdrivermanagerVersion=6.3.3&surefireVersion=3.2.5&compilerPluginVersion=3.13.0" \
  --header "Accept: */*" \
  --header "Accept-Encoding: gzip, deflate, br" \
  --header "Connection: keep-alive" \
  --header "User-Agent: EchoapiRuntime/1.1.0"

# powershell
curl.exe --request GET `
  --url "http://localhost:8080/newProject/mvn_selenium_junit5_cucumber?model=gpt-5.3-codex&projectName=ta_copilot&javaVersion=25&seleniumVersion=4.41.0&junitVersion=5.13.4&junitPlatformVersion=1.13.4&cucumberVersion=7.27.2&webdrivermanagerVersion=6.3.3&surefireVersion=3.2.5&compilerPluginVersion=3.13.0" `
  --header "Accept: */*" `
  --header "Accept-Encoding: gzip, deflate, br" `
  --header "Connection: keep-alive" `
  --header "User-Agent: EchoapiRuntime/1.1.0"  