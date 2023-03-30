FROM eclipse-temurin:8-jdk-alpine

LABEL PODAAC-CUMULUS Team

RUN apk -U upgrade \
  && apk add --repository https://dl-cdn.alpinelinux.org/alpine/v3.17/main/ --no-cache \
    nodejs-current \
    npm \
    yarn \
    curl

RUN apk add --no-cache curl tar bash procps git zip python3 py3-pip

RUN npm install -g npm@latest \
    && npm install -g snyk \
    && npm install -g snyk-to-html

# Downloading and installing Maven
ARG MAVEN_VERSION=3.6.1
ARG USER_HOME_DIR="/home/dockeruser"
ARG SHA=b4880fb7a3d81edd190a029440cdf17f308621af68475a4fe976296e71ff4a4b546dd6d8a58aaafba334d309cc11e638c52808a4b0e818fc0fd544226d952544
ARG BASE_URL=https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && echo "Downloading maven" \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  \
  && echo "Checking download hash" \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  \
  && echo "Unziping maven" \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  \
  && echo "Cleaning and setting links" \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

# Downloading and installing Gradle
# 1- Define a constant with the version of gradle you want to install
ARG GRADLE_VERSION=6.8.2

# 2- Define the URL where gradle can be downloaded from
ARG GRADLE_BASE_URL=https://services.gradle.org/distributions

# 3- Define the SHA key to validate the gradle download
#    obtained from here https://gradle.org/release-checksums/
ARG GRADLE_SHA=8de6efc274ab52332a9c820366dd5cf5fc9d35ec7078fd70c8ec6913431ee610

# 4- Create the directories, download gradle, validate the download, install it, remove downloaded file and set links
RUN mkdir -p /usr/share/gradle /usr/share/gradle/ref \
  && echo "Downlaoding gradle hash" \
  && curl -fsSL -o /tmp/gradle.zip ${GRADLE_BASE_URL}/gradle-${GRADLE_VERSION}-bin.zip \
  \
  && echo "Checking download hash" \
  && echo "${GRADLE_SHA}  /tmp/gradle.zip" | sha256sum -c - \
  \
  && echo "Unziping gradle" \
  && unzip -d /usr/share/gradle /tmp/gradle.zip \
   \
  && echo "Cleaning and setting links" \
  && rm -f /tmp/gradle.zip \
  && ln -s /usr/share/gradle/gradle-${GRADLE_VERSION} /usr/bin/gradle

# 5- Define environmental variables required by gradle
ENV GRADLE_VERSION 6.8.2
ENV GRADLE_HOME /usr/bin/gradle
ENV GRADLE_USER_HOME "$USER_HOME_DIR/.gradle"

ENV PATH $PATH:$GRADLE_HOME/bin

# Create a new user
RUN adduser --disabled-password --shell /bin/sh --home /home/dockeruser --uid 1000 dockeruser
RUN mkdir -p "$USER_HOME_DIR/.gradle"
RUN chown -R dockeruser $USER_HOME_DIR 
RUN chmod -R 777 $USER_HOME_DIR 

USER dockeruser

WORKDIR $USER_HOME_DIR

CMD ["sh"]
