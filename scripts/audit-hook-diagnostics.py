#!/usr/bin/env python3
"""Check that the advertised enrollment matches explicit source instrumentation."""
from pathlib import Path
from collections import defaultdict
import json,re,sys
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/luckyzyx/luckytool'
features=defaultdict(list)
for p in (src/'hook').rglob('*.kt'):
    for key in re.findall(r'ObservedFeature\("([^"\n]+)"',p.read_text()):
        path=str(p.relative_to(src))
        if path not in features[key]: features[key].append(path)
pilot=src/'hook/scopes/calendar/RemoveHolidayPageInformationFlow.kt'
if 'status.begin(KEY, true, tracksChanges = true)' not in pilot.read_text(): sys.exit('Missing calendar pilot')
features['remove_holiday_page_information_flow']=[str(pilot.relative_to(src))]
catalog=src/'diagnostics/HookDiagnosticCoverage.kt'
if '--generate-catalog' in sys.argv:
    catalog.write_text('package com.luckyzyx.luckytool.diagnostics\n\n/** Coverage only, never success evidence. */\nobject HookDiagnosticCoverage {\n    val keys: Set<String> = setOf(\n'+''.join('        "'+k+'",\n' for k in sorted(features))+'    )\n}\n')
keys=set(re.findall(r'^\s*"([^"\n]+)",',catalog.read_text(),flags=re.M))
if keys!=set(features): sys.exit('Catalog/enrollment mismatch: '+str(sorted(keys^set(features))))
ui='\n'.join(p.read_text() for p in (src/'ui/compose/scopes').rglob('*.kt'))
switches=set(re.findall(r'switch\(\s*key\s*=\s*"([^"\n]+)"',ui))
if any('"'+k+'"' not in ui for k in keys): sys.exit('Enrolled key absent from scope UI')
counts=defaultdict(int)
for k,p in features.items(): counts[p[0].split('/')[2]]+=1
print(json.dumps({'setting_keys':len(keys),'ui_switches':len(switches),'enrolled_switches':len(keys&switches),'hook_files':len({p for paths in features.values() for p in paths}),'scopes':dict(sorted(counts.items())),'features':dict(sorted(features.items())),'remaining_switches':sorted(switches-keys)},ensure_ascii=False,indent=2))
