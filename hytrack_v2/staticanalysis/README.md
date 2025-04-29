# Static Analysis for HyTrack

As described in the paper, we forked the pipeline for our static analysis from the IEEE Security & Privacy 2024 Paper *Tabbed Out: Subverting the Android Custom Tab Security Model* and adjusted it for our needs. You can find the original code [here](https://github.com/beerphilipp/tabbed-out).
If you are using this fork, you should consider citing the original as well:

```bibtex
@inproceedings{beer2024tabbed,
  title={Tabbed Out: Subverting the Android Custom Tab Security Model},
  author={Beer, Philipp and Squarcina, Marco and Veronese, Lorenzo and Lindorfer, Martina},
  booktitle={2024 IEEE Symposium on Security and Privacy (SP)},
  pages={105--105},
  year={2024},
  organization={IEEE Computer Society}
}
```

Our main changes include:

- Searching for TWAs usage (`is_twa_used` in tasks.py)
- Extracting of Dynamic Asset Links (`find_dal` in tasks.py)
- Extraction of registered BROWSABLE schemes (`find_schemes` in tasks.py)
- Distinction between CTs and TWAs, e.g. don't count TWA strings towards CTs, don't exit early if no CT was found.

## Structure and Usage
[Continue here](./analysis/README.md)