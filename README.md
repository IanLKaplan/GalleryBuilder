# GalleryBuilder

This is a Java application that reads [Gallery](http://galleryproject.org) directories and builds HTML for [Galleria](https://galleria.io) static gallery web sites. A copy of the Galleria JavaScript is included. This JavaScript application is licensed under the MIT open source license.  This code will only work on Gallery directories (albums) that were built using Gallery 2. Gallery 2 uses local PHP code that references local files. Later versions of Gallery stored photos and metadata in a database (e.g., mySQL). This code will not build static galleries from these newer albums.

This code is for sophisticated users, preferably who understand Java code. It builds the HTML for a Galleria gallery, but you will still have to include this HTML on a web page for the gallery. This application makes it possible to port the Gallery albums, but you will still have some work to do to rebuild your albums.

I wrote this code for the Galleria Folio gallery. This gallery is not open source and is licensed software.

For many years I hosted my personal domain bearcave.com on a Linux web host. As time went on, they migrated my web site to new servers. Unfortunately this was done without any concern for the software that my web site needed (e.g., PHP to support the Gallery albums). This broke my web site and the galleries stoped functioning.

When I mentioned this to my web host, their response was (to put it in polite terms) "too bad for you". I'll note that I was paying my web host $35/months for this "service".

Hosting static web sites on Amazon S3 is extremely inexpensive. When I built a web site for my software consulting company [Top Stone Software](http://www.topstonesoftware.com) I built the site on Amazon.  I've been meaning to move bearcave.com to Amazon to save money, but I kept putting it off since moving such a large web site is a lot of work. When my web host broke my web site, they provided a strong motivation to move.

I wrote the [S3Update](https://github.com/IanLKaplan/S3Update) application to copy and update directory trees from the local system to Amazon S3. This application makes it easier to move and maintain web sites on S3.

The last piece I needed was this applicaiton, to allow me to build static photo galleries.

Obviously these galleries are not as flexible as the Gallery based photo galleries, which allowed new photos to be added to the gallery. I may add this type of gallery in the future. I have written gallery code for the [nderground](www.nderground.net) social network and I may reuse this code in the future.

When I looked at the Gallery web site, I found that the software is no longer being supported by the original group (see [Gallery is going into hibernation](http://galleryproject.org/time-to-hibernate) ). Hopefully this software will be of use to people who are trying to migrate away from Gallery.
