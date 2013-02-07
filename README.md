geoserver-exts
==============

Building
--------

geoserver-exts currently depends on a recent version of geoserver master. You can build that first, or have the geoserver-exts build pull down the latest geoserver snapshot.

    git clone git@github.com:opengeo/geoserver-exts.git
    cd geoserver-exts
    mvn install

Adding a new extension
----------------------

1. In the parent geoserver-exts pom, add a new module to the list.
1. In the extension's pom, add a parent section to point to the parent pom.

    <pre>
    &lt;parent&gt;
      &lt;groupId&gt;org.opengeo&lt;/groupId&gt;
      &lt;artifactId&gt;geoserver-exts&lt;/artifactId&gt;
      &lt;version&gt;2.3-SNAPSHOT&lt;/version&gt;
    &lt;/parent&gt;
    </pre>
