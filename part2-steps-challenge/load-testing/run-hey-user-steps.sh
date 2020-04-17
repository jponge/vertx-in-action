#!/bin/sh
hey -z $2 \
    -o csv \
    -H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJkZXZpY2VJZCI6InAtMTIzNDU2LWFiY2RlZiIsImlhdCI6MTU4NjUyNzE1OCwiZXhwIjoxNTg3MTMxOTU4LCJpc3MiOiIxMGstc3RlcHMtYXBpIiwic3ViIjoibG9hZHRlc3RpbmctdXNlciJ9.Hqu4dheC7ifLC_EgsDlDPE_mdhjRNiHHpWNWKeXjdz8aZqR3DYfgtdID5kAOjfNzRo6M8uBWdbILOJnGmVdvp2Mi9DvUuiKfsIkyoWF9h4yitWcwv9_QxFrqL5MMl8lNXFM776W69e4Y-PZNG9-xfDF27SWMhSkXHfbN06C9JTKlbOH9qclsj_l6NVonOwqZt2DN2KjLn7RLp6HiqaszCF9m9WBuKVahPHleVeeNMUSLtOjskThCkgEipY7Wk3k947hFL4fN5OJOnV1PuEnnQ2QVaN6s6y2q6pRLMdmI-KDbSWJoE1ExvGO7wAZKOkCBsRBL1MRd-mvNqwhKSTwbXg' \
    http://$1:4000/api/v1/loadtesting-user/total \
    > data/hey-run-steps-z$2.csv
