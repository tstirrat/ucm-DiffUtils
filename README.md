DiffUtils
=========

Simple Diff Utilities for Oracle UCM

![Unified Diff Example](https://github.com/tstirrat/ucm-DiffUtils/raw/master/readme/images/diff_example.png "Unified Diff Example")

Features
--------

- Get a unified diff between two revisons.
  ![Comparing Revisions](https://github.com/tstirrat/ucm-DiffUtils/raw/master/readme/images/docinfo_compare_rev.png "Comparing Revisions")

- Get a unified diff of a content item between two UCM instances.
  ![Comparing To External Server](https://github.com/tstirrat/ucm-DiffUtils/raw/master/readme/images/docinfo_compare_ext.png "Comparing To External Server")

- (Not implemented, yet) Get a unified diff of multiple of items in batch.

Services
--------

### DIFF_REVISIONS

Compare two revisions. These do not have to be revisions of the same content item.

#### Parameters
- id1: dID of the left side
- id2: dID of the right side for comparison

### DIFF_EXTERNAL

Compare a specific revision with the latest revision of the same dDocName on another server. You must 
have a valid *outgoing* provider set up to another UCM instance.

**Note:** The *local* revision is always portrayed as the **right** side of the comparison.

#### Parameters
- dID: The local revision
- provider: The name of the outgoing provider

### (Proposed) DIFF_SEARCH_RESULTS (Not Implemented)

Compare each item returned in a search query with the latest revisions on another server.

#### Parameters
- QueryText: The UCM universal query text.
- provider: The name of the outgoing provider to compare with.

License
=======

Copyright (c) 2012 Tim Stirrat

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.