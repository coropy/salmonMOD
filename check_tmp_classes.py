import os
import sys

p = r'C:\dev\salmonMOD\tmp_classes'
log = r'C:\dev\salmonMOD\check_result.txt'

with open(log, 'w') as f:
    f.write(f'dir exists: {os.path.exists(p)}\n')
    if os.path.exists(p):
        for root, dirs, files in os.walk(p):
            f.write(f'  dir: {root}\n')
            for d in dirs:
                f.write(f'    subdir: {d}\n')
            for fl in files:
                f.write(f'    file: {fl}\n')

print('done')