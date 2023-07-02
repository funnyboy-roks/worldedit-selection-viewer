# WorldEdit Selection Viewer

A simple plugin with no config that adds particle borders for viewing one's
selected [WorldEdit](https://enginehub.org/worldedit) region.

_Note: While FAWE is not officially supported, it does seem to work_

![screenshot](https://raw.githubusercontent.com/funnyboy-roks/worldedit-selection-viewer/main/img/screenshot.gif)

## Setup

1. Download the jar
2. Put it into your `plugins` folder
3. Profit! -- No configuration necessary!

## Commands

- `/wesv colour <colour|#hex>` - Change the colour of the selection border
- `/wesv visibility <always|never|holding-tool>` - Change when the selection is visible
    - `always` - Always show the selection
    - `never` - Never show the selection
    - `holding-tool` - Show the selection when holding the select tool (default: Wooden Axe)

## Permissions

- `worldedit-selection-viewer.view` - If the player can see the borders
    - Default: Enabled for all players
- `worldedit-selection-viewer.command` - If the player can run the `/wesv` command
    - Default: Enabled for all players
