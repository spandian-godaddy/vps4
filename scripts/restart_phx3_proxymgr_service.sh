#!/usr/bin/env bash
ssh 02586d6e7eecm@p3plvps4wp01.cloud.phx3.gdg `sudo systemctl restart proxymgr`;
ssh 02586d6e7eecm@p3plvps4wp02.cloud.phx3.gdg `sudo systemctl restart proxymgr`;