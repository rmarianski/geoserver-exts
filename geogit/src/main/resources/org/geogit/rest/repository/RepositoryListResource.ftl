<html>
<head>
<title> GeoGit Web API </title>
<meta name="ROBOTS" content="NOINDEX, NOFOLLOW"/>
</head>
<body>
<h2>Geogit repositories</h2>
<#if repositories?size != 0>
<ul>
<#foreach repo in repositories>
<li><a href="${page.pageURI(repo)}">${repo}</a></li>
</#foreach>
</ul>
<#else>
<p>There are no Geogit DataStores configured and enabled.</p>
</#if>
</body>
</html>
