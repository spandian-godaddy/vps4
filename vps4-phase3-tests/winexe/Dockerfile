# This image uses patched version of winexe which uses SMB2 instead of the deprecated SMB1.
# The binary was originally downloaded from http://dl-openaudit.opmantek.com/winexe-static

# Build with:
# docker build --tag winexe:1.1 .

# Launch a container with:
# docker run -it --name winexe winexe:1.1

# Run a single command with:
# docker run -it --rm winexe:1.1 winexe --reinstall -U 'username'%'password' //127.0.0.1 'hostname'

FROM debian:latest

WORKDIR /root
COPY winexe-static /usr/bin/winexe
RUN chmod +x /usr/bin/winexe

CMD /bin/bash
