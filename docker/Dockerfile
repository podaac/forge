FROM amazoncorretto:11

RUN yum -y update
RUN yum install -y wget
RUN yum install -y unzip
RUN yum install -y /usr/sbin/adduser

RUN groupadd dockergroup
RUN useradd -m -g dockergroup dockeruser

WORKDIR /home/dockeruser
COPY . /home/dockeruser

# Download and install Gradle
RUN wget https://services.gradle.org/distributions/gradle-6.3-bin.zip && \
    unzip gradle-6.3-bin.zip && \
    rm gradle-6.3-bin.zip

# Add Gradle binaries to the PATH
ENV PATH="/home/dockeruser/gradle-6.3/bin:${PATH}"

# Display Gradle version to verify the installation
RUN gradle --version

RUN chown dockeruser:dockergroup -R /home/dockeruser

USER dockeruser

RUN ./gradlew shadowJar

RUN wget https://github.com/nasa/cumulus-message-adapter/releases/download/v2.0.3/cumulus-message-adapter.zip
RUN mkdir cumulus-message-adapter
RUN unzip -d cumulus-message-adapter cumulus-message-adapter.zip

ENV CUMULUS_MESSAGE_ADAPTER_DIR=/home/dockeruser/cumulus-message-adapter

ENTRYPOINT []
