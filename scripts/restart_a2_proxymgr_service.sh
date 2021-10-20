#!/usr/bin/env bash
ssh 02586d6e7eecm@a2plvps4wp01.cloud.iad2.gdg `sudo systemctl restart proxymgr`;
ssh 02586d6e7eecm@a2plvps4wp02.cloud.iad2.gdg `sudo systemctl restart proxymgr`;