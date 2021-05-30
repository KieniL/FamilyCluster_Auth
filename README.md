# authservice


package the project with this command before the container can be built

mvn clean package

For auth create a new user with create databases, can login permissions and a pw. <br/>
Also create a new db and set the new user as owner. This will only be used for auth.


There are several things tested:
* Anchore (on Docker image)
* Secrets in Git (Trufflehog which also checks Git commits)
* Owasp Dependency Check
* Checkstyles (check styles on java e.g lines not too long and more readability)
* sonar scanning
* mvn test + jacoco (unit Test coverage) --> jacoco plugin is needed
* spotbugs (own maven plugin)
* kube-score (benchmark checking of kubenetes files)
* kube-val (validates kubernets files)
