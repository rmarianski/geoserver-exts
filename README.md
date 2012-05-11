geoserver-exts
==============

Building
--------

geoserver-exts currently depends on the suite 2.5 version of geoserver. Build that first:

    git clone git@github.com:opengeo/geoserver.git
    cd geoserver
    git checkout suite-2.5
    cd src/web/app
    ln -s ../../../data/minimal .
    cd ../../..
    mvn clean install -DskipTests

Now the geoserver extensions can be built. Navigate to the root of the extensions and run:

    mvn test

Adding a new extension
----------------------

1. In the parent geoserver-exts pom, add a new module to the list.
1. In the extension's pom, add a parent section to point to the parent pom.

    <pre>
    &lt;parent&gt;
      &lt;groupId&gt;org.opengeo&lt;/groupId&gt;
      &lt;artifactId&gt;geoserver-exts&lt;/artifactId&gt;
      &lt;version&gt;${project.version}&lt;/version&gt;
    &lt;/parent&gt;
    </pre>
