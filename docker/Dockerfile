FROM amazoncorretto:21-al2023

# AL2023 uses dnf
RUN dnf -y update \
 && dnf -y install wget unzip shadow-utils tar gzip python3.12 \
 && dnf clean all

 RUN dnf install -y findutils && dnf clean all

# Make python3 resolve to Python 3.12 for shell/runtime use
# (do not remove system python packages)
RUN ln -sf /usr/bin/python3.12 /usr/local/bin/python3

RUN groupadd dockergroup
RUN useradd -m -g dockergroup dockeruser

WORKDIR /home/dockeruser
COPY . /home/dockeruser

# Gradle is provided by the project wrapper (./gradlew, Gradle 9.9.0, checksum-verified
# via gradle-wrapper.properties), so no separate Gradle install is needed.

RUN chown dockeruser:dockergroup -R /home/dockeruser

USER dockeruser

RUN ./gradlew shadowJar

RUN wget https://github.com/nasa/cumulus-message-adapter/releases/download/v2.0.6/cumulus-message-adapter.zip
RUN mkdir cumulus-message-adapter
RUN unzip -d cumulus-message-adapter cumulus-message-adapter.zip

ENV CUMULUS_MESSAGE_ADAPTER_DIR=/home/dockeruser/cumulus-message-adapter

ENTRYPOINT []
