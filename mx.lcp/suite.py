suite = {
  "mxversion" : "5.175.4",
  "name" : "lcp",
  "versionConflictResolution" : "latest",

  "javac.lint.overrides" : "none",

  "imports" : {
    "suites" : [
      {
        "name" : "common",
        "version" : "2daf1bc1b0c5a2f40193897e9eaeaa662266ef60",
        "urls" : [
          { "url" : "https://github.com/unknown-technologies/common", "kind" : "git" }
        ]
      }
    ]
  },

  "licenses" : {
    "GPLv3" : {
      "name" : "GNU General Public License, version 3",
      "url" : "https://opensource.org/licenses/GPL-3.0",
    },
  },

  "defaultLicense" : "GPLv3",

  "projects" : {
    "com.unknown.emulight.lcp" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "common:CORE",
        "common:AUDIO",
        "common:NET",
        "common:PLATFORM",
        "common:MOTIF",
        "common:WINDOWS"
      ],
      "javaCompliance" : "21+",
      "workingSets" : "lcp",
      "license" : "GPLv3",
    }
  },

  "distributions" : {
    "LCP" : {
      "path" : "build/lcp.jar",
      "subDir" : "lcp",
      "sourcesPath" : "build/lcp.src.zip",
      "mainClass" : "com.unknown.emulight.lcp.ui.MainWindow",
      "dependencies" : [
        "com.unknown.emulight.lcp"
      ],
      "overlaps" : [
        "common:CORE",
        "common:AUDIO",
        "common:NET",
        "common:PLATFORM",
        "common:MOTIF",
        "common:WINDOWS"
      ]
    }
  }
}
