Email Graph Viewer
==================


Java 
-----

Requires profusion and prefuse (included)

Java version is written prefuse and will either read from a file or read from a
TCP port.  The easiest way to get it to read from a file is to  do the following

On one terminal run `nc -l 4321 > dump` on another terminal run
 `./email_backend.py <mboxfile>` let it finish and then run make.
This will compile a jar file with the dump file as a resource. 

To use the TCP port, run `make empty`.  And then you can use the
`email_backend.py` file in the same manner


Python
------- 

Requires a newish version of django

go into the emailviewer directory and run `./manage.py runserver`
Then browse [http://localhost:8000/viz](http://localhost:8000/viz). 

mbox files should be place in the data folder.

The important code is in `emailviewer/viz/views.py` and `emailviewer/viz/templates/viz/network.html`.

