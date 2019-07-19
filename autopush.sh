#!/bin/bash

echo "new stuff" >> ./README.md
git commit -a -m "$RANDOM"
git push
