# What is Euphony ?

Euphony is a __unifier of malware labels__.

From a list of [VirusTotal](https://www.virustotal.com/) reports, Euphony can parse malware labels and produce a single family per file.

# Installation

Euphony is available both as [a single jar](https://github.com/fmind/euphony/releases) and [from
sources](https://github.com/fmind/euphony/).

For end users, the single jar is recommended.

# Usage

    $ java -jar euphony.jar [args]

## Options

* -h, --help: Display a help summary with acceptable arguments and options.
* -l, --log-level LEVEL: Set the log level of the program (default: warn)
* -m, --max-turn VALUE: Set the maximum number of complete iteration for inference at the parsing stage.
* -t, --threshold VALUE: Set the threshold value for the trimming operation at the clustering stage.
* -e, --export-dir DIR: Set the output directory of the program (default: current directory)
* -f, --field FIELD: Set the label field to cluster and export (from: type, platform, family,
  default: family)
* -r, --reports-file FILE: Provide a sequence of reports from VirusTotal formatted as JSON records
  (one per line).
* -g, --ground-file FILE: Provide a ground-truth to evaluate the output formatted as JSON records.
* -s, --seeds-file FILE: Provide a seeds file with some initial domain knowledge about malware
  formatted as an EDN structure
  (default: resources/seed-max.edn).
* -d, --database-uri: URI Provide a database URI to run the program and persist the learning
  (default: no persistence).
* -A, --export-all: export every information below
* -E, --export-election: field frequency per malware signature
* -O, --export-proposed: best candidate per malware signature
* -P, --export-parse-rules: associations between label and field
* -T, --export-parse-mapping: tokenization of malware labels
* -V, --export-vendor-reports: output dataset after parsing
* -G, --export-cluster-graph: output graph after clustering
* -C, --export-cluster-rules: associations between raw field and clustered field
* -D, --export-cluster-mapping: clustering of malware fields
* -R, --export-cluster-reports: output dataset after clustering
* -M, --export-malstats: statistics about malware files
* -F, --export-famstats: statistics about malware families

# Examples

    $ java -jar euphony.jar -e output-dir/ -r reports.vt -CPEO

    $ java -jar euphony.jar -e output-dir/ -r reports.vt -t 0.05 -CPEO

    $ java -jar euphony.jar -e output-dir/ -r reports.vt -f type -CPEO

    $ java -jar euphony.jar -e output-dir/ -r reports.vt -g truths.gt -CPEOMF

## Report file (with two items)

{"positives": 2, "resource": "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4", "verbose_msg": "Scan finished, information embedded", "scans": {"NANO-Antivirus": {"result": null, "version": "1.0.38.8984", "detected": false, "update": "20160713"}, "AVware": {"result": "Trojan.AndroidOS.Generic.A", "version": "1.5.0.42", "detected": true, "update": "20160713"}, "ESET-NOD32": {"result": "Android/Adrd.A", "version": "13792", "detected": true, "update": "20160712"}}, "sha1": "09b143b430e836c513279c0209b7229a4d29a18c", "total": 55, "scan_id": "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4-1468430330", "permalink": "https://www.virustotal.com/file/5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4/analysis/1468430330/", "sha256": "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4", "scan_date": "2016-07-13 17:18:50", "md5": "c05c25b769919fd7f1b12b4800e374b5", "response_code": 1}
{"positives": 1, "resource": "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d", "verbose_msg": "Scan finished, information embedded", "scans": {"Tencent": {"result": "a.remote.adrd", "version": "1.0.0.1", "detected": true, "update": "20160707"}}, "sha1": "32cd5dbef434b926ce34e89f0d185fe8d1b5fdfb", "total": 54, "scan_id": "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d-1467894540", "permalink": "https://www.virustotal.com/file/2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d/analysis/1467894540/", "sha256": "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d", "scan_date": "2016-07-07 12:29:00", "md5": "39c1bfbb62687e1b1d2bc4d273600448", "response_code": 1}

## Ground-truth file (with two items)

{"resource": "f63256cf4eef0a60fe56989b1474dd9b0b2bb580ce9fd262b18592bf0506f911", "name": "Adwo", "type": "adware", "platform": "android"}
{"resource": "a9cbe3e3d446cea683c1e72f2994f40024afed1bb1186b27690ff21741046312", "name": "Dowgin", "type": "trojan", "platform": "linux"}
