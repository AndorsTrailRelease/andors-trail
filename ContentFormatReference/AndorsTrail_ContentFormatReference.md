# Content Format Reference
by Zukero » Fri Aug 11, 2017 1:01 pm

Hello adventurers!

This thread compiles a reference documentation of all files format used to define game content.
It can be of help to users of ATCS, Tiled, but also Notepad++ and VIM madmen alike.

There is a lot to read here, and I don't expect anyone to read it as a book. You'll most likely want to Ctrl+F your way into it to get what you need, when you need it. It not only describes the format and the keywords used, but also the effect each value can have on the game engine. Keywords are typically "**in bold between quotes**".

For all game object types, I included an example that make use of all the possible fields, in order to give a concrete example of all syntax.

You'll have to know a little bit about the XML and JSON formats for this to make sense on its own, but Google is your friend for those.

## Table of contents

**[Maps](#Maps-format)**   
**[Worldmaps](#Worldmaps-format)**  
**[Actor conditions](#Actor-conditions-format)**  
**[Dialogues](#Dialogues-format)**  
**[Droplists](#Droplists-format)**  
**[Items](#Items-format)**  
**[Item categories](#Item-categories-format)**  
**[NPCs](#NPCs-format)**  
**[Quests](#Quests-format)**  


Revised by Nut in March 2023:
  - Migration to git repo https://github.com/AndorsTrailRelease/andors-trail/tree/master/ContentFormatReference
  - Additions to the engine
  - Small fixes



## Maps format.

**Maps use the TMX format defined by the [Tiled editor](https://mapeditor.org). Each map is defined in a single-dedicated TMX file.**

![tiled-icon](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/tiled-icon.png)
The name of the file is the map's ID. If the file is named "blackwater_mountain0.tmx", its ID will be "blackwater_mountain0".

Different components of a map can be edited with either Tiled or ATCS. For each part, I'll give the recommended tool, but overall I suggest to always start Tiled from within ATCS, as it eases editing back and forth with both tools.

Several tags can have a "properties" sub-tag, itself containing any number of "property" sub-tags. Each of the "property" sub-tags must contain two attributes: "name" and "value". For readability of this document, when you see that a tag can have some property named "X" with value "Y", it means that the tag has a "properties" sub-tag, itself containing a "property" tag that has an attribute "name" with value "X" and an attribute "value" with value "Y".

The TMX format is based on XML, with a "map" top-level tag. 
- A "**map**" tag must have the following attribute values:
  - **orientation**="orthogonal"
  - **tilewidth**="32"
  - **tileheight**="32"
  - **width**="X" (X ranging from 1 to 32, max 30 preferred)
  - **height**="Y" (Y ranging from 1 to 32, max 30 preferred)

- The "**map**" tag will have the following sub-tags:
  - "**properties**" optionally (details below)
  - "**tileset**" in large quantity. They should match exactly the one in template.tmx.
  - "**layer**", at least 4 of them (details below)
  - "**objectgroup**", at least 1 (details below)

- A map can have the following global properties. You can edit this in Tiled using the "Map"->"Map properties" menu, but ATCS is the recommended tool.
  - "**outdoors**" as the property name, with "1" as the value indicate that the map is outdoors. Unused by the game engine for now. A value of "0" is possible too, but as it is the default, you can remove the property entirely in this case.
  - "**colorfilter**" as the property name, which impact the way everything looks on this map, player and NPCs included, with one of the following values:
    - "**none**" (the default, if so, you can omit the whole tag)
    - "**black20**" (adds a subtle dim to the map, making it a bit darker)
    - "**black40**" (adds a visible dim to the map, making it darker)
    - "**black60**" (adds a strong dim to the map, making it a lot darker)
    - "**black80**" (adds a very strong dim to the map, making it almost black)
    - "**invert**" (everything is drawn with inverted colors, use with caution)
    - "**bw**" (everything is drawn in black & white, use with caution)
    - "**redtint**" (tints everything in red, useful for a burning cave glow)
    - "**greentint**" (tints everything in green, useful for... I don't really know yet)
    - "**bluetint**" (tints everything in blue, could be used for that moonlit night lightning effect).


![tile_layer](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/tile_layer.png)
**The "layer" tags define the visual aspect of the map. Tiled is the recommended tool to edit all maps graphics, but I recommend you to launch Tiled from ATCS**

- The "**layer**" tag must have "**width**" and "**height**" attributes, both matching the values of the "map" top-level tag. It must also have a "**name**" attribute and a "**data**" sub-tag. 

- :!: The "**data**" sub-tag must have the "**encoding**" attribute set to "**base64**" and the "**compression**" attribute set to "**zlib**". The content of the data tag itself should only be edited with Tiled. 

- There must be at least 4 "**layer**" tags, one for each of the following values for the "**name**" attribute: "**Ground**", "**Objects**", "**Above**", and "**Walkable**".
Supplementary "layer" tags can have any other name, and are used for replace areas (details below). I'll call these "extra graphical layers" in the rest of the document.

- Note that the "**Walkable**" graphical layer is never displayed in-game. Any empty tile in this layer can be walked on by the player, while non-empty ones cannot. By convention, **we always use the top-left sprite from the "map_trail_1" **spritesheet to mark the unwalkable tiles, but technically, any sprite would do.


![object_layer](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/object_layer.png)
**The "objectgroup" tags are containers for the different map objects. Think of them as folders. Both ATCS and Tiled can be used to edit these, as ATCS cannot do some things that Tiled can, like reordering the "objectgroup"s and moving a map object from one "objectgroup" to another.**

The "objectgroup" tag must have a "name" attribute, with any textual value (spaces discouraged), although by convention (and as provided in the template.tmx map), we use "Spawn" to store spawn areas (details below), "Key" for map objects of the "key" type, and "Mapevents" for the rest (like mapchanges, containers, beds...)

If you intend to enable or disable map objects dynamically (through a quest/dialogue/script), create an "**objectgroup**" dedicated to a group of map objects that will be enabled or disabled together. This applies to all types of map objects except spawn areas, as these are handled differently and individually (per spawn area, not per group) by the dialogue/script system.

- An "**objectgroup**" can have the following attribute:
  - "**visible**", with a 0 or 1 value. 0 indicates the the layer is hidden in Tiled, or in the map view of ATCS, but has no effect on the game. 1 is the default value, and the attribute can be omitted completely.

- An "**objectgroup**" can have the following property:
  - "**active**", with the value "false" or "true". "true" being the default, the tag can be omitted completely in this case. When such a property is present, any map object within this object group (except spawn areas) will not be active until enabled by a script/dialogue.


- An "**objectgroup**" tag can contain any number of "object" sub-tags


**The "object" sub-tag of an "objectgroup" tag describe one map object, some area that is invisible to the user, but tells the game engine what can happen there. These should be created and edited with ATCS, but Tiled can be used if you really know what you are doing.**

- An "**object**" tag must have the following attributes:
  - "**x**" with an integer value, in pixels, that indicate the top-left X coordinate of the area. It must be a multiple of 32.
  - "**y**" with an integer value, in pixels, that indicate the top-left Y coordinate of the area. It must be a multiple of 32.
  - "**width**" with an integer value, in pixels, that indicate the width of the area. It must be a multiple of 32.
  - "**height**" with an integer value, in pixels, that indicate the height of the area. It must be a multiple of 32.
  - "**type**" with a textual value that indicate the type of game object it describes (see possible values below, with the details of each object type).
  - "**name**" with a textual value. Depending on the type, some constraints may apply (details below for each object type).


![container](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/container.png)
**If the "type" attribute of the "object" tag has the value "**container**", the player will receive the contents of a droplist upon stepping on it. Moreover, items dropped in such a location will be hidden (no loot bag visible).**
- A "**container**" must have the following attribute in the "object" tag:
  - "**name**" with a textual value that equals an existing droplist's ID.


![key](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/key.png)
**If the "type" attribute of the "object" tag has the value "**key**", the player will be granted access only if he satisfies a given requirement, otherwise, a script will be run (and eventually, a message displayed).**

- A "**key**" must have the following attribute in the "object" tag:
  - "**name**" with a textual value that can be anything. ATCS Uses "Object" by default, and doesn't allow you to change it. It will preserve changes made to it in Tiled though.

- A "**key**" must have the following properties
  - "**phrase**" with a textual value that equals an existing dialogue's ID. This dialogue can do anything a normal dialogue with a NPC can, including giving rewards, checking requirements, talk to the player, give choices. If this dialogue "says" something to the player, it will be displayed, but this is not mandatory.
  - All the attributes of a requirement (details below). If this requirement is satisfied, the player can enter the area, otherwise, the dialogue defined by the "phrase" property is run.


![tiled-icon](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/tiled-icon.png)
**If the "type" attribute of the "object" tag has the value "**mapchange**", the player will be transported to another "**mapchange**" area upon entering the area. When making transitions between two adjacent maps, it is important that the two reciprocal mapchanges have the same width or height (the other being 1 tile).**

- A "**mapchange**" must have the following attribute:
  - "**name**" with a textual value, that will be the ID of this mapchange area. It must be unique within this map.

- A "**mapchange**" should have the following properties (Optional. A mapchange without these properties is an arrival only, and does nothing upon entering it):
  - "**map**" with a textual value that matches the ID of the target map. It can be the current map's ID too.
  - "**place**" with a textual value that matches the ID (the "name") of a mapchange area in the target map.


![replace](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/replace.png)
**If the "type" attribute of the "object" tag has the value "**replace**", the visual appearance of the covered area of the map will be modified if the player satisfies a given requirement. Note that in order to revert a map to its original appearance later, you also need to create a second replace area, with dedicated graphical layers that are copies of the original layers. The creation of replace areas is best done with ATCS, but the graphical layers must still be edited with Tiled.**

- A "**replace**" must have the following attribute:
  - "**name**" with any textual value. I try to give it a meaningful name, and suffix all associated graphical layers with that name, but this is only a recommendation.

- A "**replace**" can have any (at least one) of the following properties:
  - "**Ground**" with a textual value that matches the ID of an extra graphical layer. When the requirement is satisfied, the portion of the "Ground" graphical layer covered by this replace area will be replaced by the content of the target extra graphical layer.
  - "**Objects**". Same as above for the "Objects" graphical layer.
  - "**Above**". Same as above for the "Above" graphical layer.
  - "**Walkable**". Same as above for the "Walkable" graphical layer. 

- A "**replace**" must have the following properties:
  - All the attributes of a requirement (details below). 


![rest](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/rest.png)
**If the "type" attribute of the "object" tag has the value "**rest**", the player is offered to rest upon entering this area. Beds are implemented this way.**

- A "**rest**" must have the following attribute:
  - "**name**" with any textual value. Try to use a unique name within a map.


![script](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/script.png)
**If the "type" attribute of the "object" tag has the value "**script**", a dialogue/script will be evaluated when the player is in the area. The creation of script areas is best done with ATCS.**

- A "**script**" must have the following attribute:
  - "**name**" with a textual value that equals an existing dialogue's ID. Just like the "phrase" property of a key area, this dialogue can do anything a normal dialogue with a NPC can, including giving rewards, checking requirements, talk to the player, give choices. If this dialogue "says" something to the player, it will be displayed, but this is not mandatory.

- A "**script**" must have the following property:
  - "**when**" with one of the following textual values: "enter", "step", "round", "always".
    - If the value is "**enter**", the script is evaluated once each time the player enters the area, including during battle (fleeing).
    - If the value is "**step**", the script is evaluated each time the player moves within the area (when the target tile of the movement is in the area), including upon entering the area and during battle (fleeing).
    - If the value is "**round**", the script will be evaluated on each new round as long as the player is in the area, including during battle.
    - If the value is "**always**", the script will be evaluated on each game tick (~500ms) while the player is in the area. Use with caution, including during battle.


![sign](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/sign.png)
**If the "type" attribute of the "object" tag has the value "**sign**", a dialogue/script will be evaluated when the player enters the area. They are used for signposts or graves for example. It is equivalent to a script area with a "enter" trigger. Sign areas are typically 1x1 tile large, and have simple scripts that display information about the current location. The creation of sign areas is best done with ATCS.**

- A "**sign**" must have the following attribute:
  - "**name**" with a textual value that equals an existing dialogue's ID. Just like the "phrase" property of a key area, this dialogue can do anything a normal dialogue with a NPC can, including giving rewards, checking requirements, talk to the player, give choices. If this dialogue "says" something to the player, it will be displayed, but this is not mandatory.


![npc](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/npc.png)
**If the "type" attribute of the "object" tag has the value "**spawn**", NPCs or monsters will appear in the area. Except for some monsters with specific movement types, NPCs and monsters can roam their spawn areas, but can't leave it. The creation of spawn areas is best done with ATCS.**

- A "**spawn**" must have the following attribute:
  - "**name**" with any textual value. This value MUST be unique within the map, as it is used by scripts to activate or deactivate these areas. This ID is also used in the saves.

- A "**spawn**" must have the following property:
  - "**spawngroup**" with a textual value matching either a valid "spawngroup" id (these do not exist as stand-alone objects, but are indicated in a NPC's description), either a NPC id. In the case of a spawngroup, each spawned NPC will be randomly selected among all the NPCs that have the matching "spawngroup" value.

- A "**spawn**" can have the following properties:
  - "**quantity**" with a numerical value. This represents the total number of monsters that can be spawned in the area. There's no use in making this number larger than the number of walkable tiles in the spawn area.
  - "**active**", with the value "false" or "true". "true" being the default, the tag can be omitted completely in this case. When the value is "false", this spawn area will not be active until enabled by a script/dialogue, and no NPC will be spawned.
  - "**ignoreAreas**", with the value "false" or "true". "false" being the default, the tag can be omitted completely in this case. When the value is "true", NPCs spawned by this area can be spawned in, and move into, overlapping "key", "rest" and "mapchange" areas.


**"replace" and "key" areas need the properties of a requirement.**

- A requirement is defined by the following properties:
  - "**requireType**" with one of the following textual values: "**questProgress**", "**questLatestProgress**", "**inventoryRemove**", "**inventoryKeep**", "**wear**", "**skillLevel**", "**killedMonster**", "**timerElapsed**", "**usedItem**", "**spentGold**", "**consumedBonemeals**", "**hasActorCondition**", "**factionScore**, "**factionScoreEquals**", "**random**", "**date**", "**dateEquals**", "**time**", "**timeEquals**".
  - "**requireID**" with a textual value. The specifities of this property depends on the selected "requireType", and will be detailed below.
  - "**requireValue**" with a textual value. The specifities of this property depends on the selected "requireType", and will be detailed below.
  - "**requireNegation**" with the value true or false. false being the default, this property can be omitted completely in this case. When the value is true, the required is fulfilled only when it shouldn't be.

- Some details depending on different requireTypes:
  - When "requireType" is "**questProgress**" or "**questLatestProgress**", the "requireID" property must have a value matching a quest ID, and the 
"requireValue" must have a value matching a quest step ID. In both case, the player must have reached this quest stage in order to fulfill the requirement. The difference between "questProgress" and "latestQuestProgress" is that the latter also requires that no other quest stage with a greater ID must have been reached (numerical comparison between the stage IDs).

  - When "requireType" is "**inventoryRemove**", "**inventoryKeep**" or "**usedItems**", the "requireID" property must have a value matching an item ID, and the "requireValue" must have a positive numerical value depicting the quantity of the item. In the case of the "**usedItems**" type, the player must have used at least the selected quantity of the selected item type (quaffed X potions, eat Y apples...). In the case of "**inventoryKeep**" and "**inventoryRemove**", the player must have at least the selected quantity of the selected item in his inventory, the difference is that with the latter, these items will be taken from the player. I'd suggest avoiding doing that outside of a dialogue though, or with proper warning.

  - When "requireType" is "**wear**", the "requireID" property must have a value matching an item ID. "requireValue" is not used. To fulfill this, the player must have the selected item equipped.

  - When "requireType" is "**skillLevel**", the "requireID" property must have a value matching a skill ID, as found here: [url]https://github.com/Zukero/andors-trail/blob/master/AndorsTrail/src/com/gpl/rpg/AndorsTrail/model/ability/SkillCollection.java[/url]. The "requireValue" property must have a numerical value that indicate the level of the skill that is needed to fulfill the requirement.

  - When "requireType" is "**killedMonster**", the "requireID" property must have a value matching a NPC ID, and the "requireValue" property must be a numerical value that indicate the number of the given NPC that the player must have killed.

  - When "requireType" is "**timerElapsed**", the "requireID" property must be a textual value matching the ID of a timer started by a dialogue. The "requireValue" must be a numerical value indicating the number of game rounds that must have passed since the timer was last started.

  - When "requireType" is "**factionScore**" or "**factionScoreEquals**", the "requireID" property must be a textual value matching the ID of a faction whose score was changed by a dialogue using the "alignmentChange" reward. The "requireValue" must be a numerical value (positive or negative) indicating the minimum or exact score the player must have for this faction to fulfill this requirement.

  - When "requireType" is "**spentGold**", the "requireID" property is unused, and the "requireValue" must be a numerical value indicating the total amount of gold that the player must have spent overall.

  - When "requireType" is "**consumedBonemeals**", the "requireID" property is unused, and the "requireValue" must be a numerical value indicating the total amount of bonemeal potions (all kind of bonemeals, including Lodar's) that the player must have consumed overall.

  - When "requireType" is "**hasActorConditions**", the "requireID" property must have a value matching an actor condition ID. The "requireValue" property is unused. The requirement is fulfilled when the player is afflicted by the selected actor condition.

**Full example using all tags and attributes.**
<details>
<summary>Example map - click to open</summary>

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE map SYSTEM "http://mapeditor.org/dtd/1.0/map.dtd">
<map version="1.0" orientation="orthogonal" width="30" height="30" tilewidth="32" tileheight="32">
 <properties>
  <property name="colorfilter" value="black20"/>
  <property name="outdoors" value="1"/>
 </properties>
 <tileset firstgid="1" name="map_bed_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_bed_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="129" name="map_border_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_border_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="257" name="map_bridge_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_bridge_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="385" name="map_bridge_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_bridge_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="513" name="map_broken_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_broken_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="641" name="map_cavewall_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_cavewall_1.png" width="576" height="192"/>
 </tileset>
 <tileset firstgid="749" name="map_cavewall_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_cavewall_2.png" width="576" height="192"/>
 </tileset>
 <tileset firstgid="857" name="map_cavewall_3" tilewidth="32" tileheight="32">
  <image source="../drawable/map_cavewall_3.png" width="576" height="192"/>
 </tileset>
 <tileset firstgid="965" name="map_cavewall_4" tilewidth="32" tileheight="32">
  <image source="../drawable/map_cavewall_4.png" width="576" height="192"/>
 </tileset>
 <tileset firstgid="1073" name="map_chair_table_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_chair_table_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="1201" name="map_chair_table_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_chair_table_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="1329" name="map_crate_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_crate_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="1457" name="map_cupboard_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_cupboard_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="1585" name="map_curtain_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_curtain_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="1713" name="map_entrance_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_entrance_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="1841" name="map_entrance_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_entrance_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="1969" name="map_fence_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_fence_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="2097" name="map_fence_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_fence_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="2225" name="map_fence_3" tilewidth="32" tileheight="32">
  <image source="../drawable/map_fence_3.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="2353" name="map_fence_4" tilewidth="32" tileheight="32">
  <image source="../drawable/map_fence_4.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="2481" name="map_ground_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_ground_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="2609" name="map_ground_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_ground_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="2737" name="map_ground_3" tilewidth="32" tileheight="32">
  <image source="../drawable/map_ground_3.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="2865" name="map_ground_4" tilewidth="32" tileheight="32">
  <image source="../drawable/map_ground_4.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="2993" name="map_ground_5" tilewidth="32" tileheight="32">
  <image source="../drawable/map_ground_5.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="3121" name="map_ground_6" tilewidth="32" tileheight="32">
  <image source="../drawable/map_ground_6.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="3249" name="map_ground_7" tilewidth="32" tileheight="32">
  <image source="../drawable/map_ground_7.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="3377" name="map_ground_8" tilewidth="32" tileheight="32">
  <image source="../drawable/map_ground_8.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="3505" name="map_house_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_house_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="3633" name="map_house_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_house_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="3761" name="map_indoor_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_indoor_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="3889" name="map_indoor_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_indoor_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="4017" name="map_kitchen_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_kitchen_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="4145" name="map_outdoor_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_outdoor_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="4273" name="map_pillar_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_pillar_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="4401" name="map_pillar_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_pillar_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="4529" name="map_plant_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_plant_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="4657" name="map_plant_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_plant_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="4785" name="map_rock_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_rock_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="4913" name="map_rock_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_rock_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="5041" name="map_roof_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_roof_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="5169" name="map_roof_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_roof_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="5297" name="map_roof_3" tilewidth="32" tileheight="32">
  <image source="../drawable/map_roof_3.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="5425" name="map_shop_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_shop_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="5553" name="map_sign_ladder_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_sign_ladder_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="5681" name="map_table_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_table_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="5809" name="map_trail_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_trail_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="5937" name="map_transition_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_transition_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="6065" name="map_transition_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_transition_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="6193" name="map_transition_3" tilewidth="32" tileheight="32">
  <image source="../drawable/map_transition_3.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="6321" name="map_transition_4" tilewidth="32" tileheight="32">
  <image source="../drawable/map_transition_4.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="6449" name="map_transition_5" tilewidth="32" tileheight="32">
  <image source="../drawable/map_transition_5.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="6577" name="map_tree_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_tree_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="6705" name="map_tree_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_tree_2.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="6833" name="map_wall_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_wall_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="6961" name="map_wall_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_wall_2.png" width="480" height="256"/>
 </tileset>
 <tileset firstgid="7081" name="map_wall_3" tilewidth="32" tileheight="32">
  <image source="../drawable/map_wall_3.png" width="480" height="256"/>
 </tileset>
 <tileset firstgid="7201" name="map_wall_4" tilewidth="32" tileheight="32">
  <image source="../drawable/map_wall_4.png" width="480" height="256"/>
 </tileset>
 <tileset firstgid="7321" name="map_window_1" tilewidth="32" tileheight="32">
  <image source="../drawable/map_window_1.png" width="512" height="256"/>
 </tileset>
 <tileset firstgid="7449" name="map_window_2" tilewidth="32" tileheight="32">
  <image source="../drawable/map_window_2.png" width="512" height="256"/>
 </tileset>
 <layer name="Ground" width="30" height="30">
  <data encoding="base64" compression="zlib">
   eJztwQEBAAAAgiD/r25IQAEAAPBoDhAAAQ==
  </data>
 </layer>
 <layer name="Objects" width="30" height="30">
  <data encoding="base64" compression="zlib">
   eJztwQEBAAAAgiD/r25IQAEAAPBoDhAAAQ==
  </data>
 </layer>
 <layer name="Above" width="30" height="30">
  <data encoding="base64" compression="zlib">
   eJztwQEBAAAAgiD/r25IQAEAAPBoDhAAAQ==
  </data>
 </layer>
 <layer name="Walkable" width="30" height="30">
  <data encoding="base64" compression="zlib">
   eJztwQEBAAAAgiD/r25IQAEAAPBoDhAAAQ==
  </data>
 </layer>
 <layer name="Ground_2" width="30" height="30">
  <data encoding="base64" compression="zlib">
   eJztwQEBAAAAgiD/r25IQAEAAPBoDhAAAQ==
  </data>
 </layer>
 <layer name="Objects_2" width="30" height="30">
  <data encoding="base64" compression="zlib">
   eJztwQEBAAAAgiD/r25IQAEAAPBoDhAAAQ==
  </data>
 </layer>
 <layer name="Above_2" width="30" height="30">
  <data encoding="base64" compression="zlib">
   eJztwQEBAAAAgiD/r25IQAEAAPBoDhAAAQ==
  </data>
 </layer>
 <layer name="Walkable_2" width="30" height="30">
  <data encoding="base64" compression="zlib">
   eJztwQEBAAAAgiD/r25IQAEAAPBoDhAAAQ==
  </data>
 </layer>
 <objectgroup name="Mapevents">
  <object name="mapchange_id" type="mapchange" x="256" y="0" width="224" height="32">
   <properties>
    <property name="map" value="map_id"/>
    <property name="place" value="mapchange_id"/>
   </properties>
  </object>
  <object name="bed_id" type="rest" x="544" y="160" width="32" height="32"/>
  <object name="dialogue_id" type="script" x="544" y="0" width="32" height="32">
   <properties>
    <property name="when" value="enter"/>
   </properties>
  </object>
  <object name="dialogue_id" type="script" x="576" y="0" width="32" height="32">
   <properties>
    <property name="when" value="step"/>
   </properties>
  </object>
  <object name="dialogue_id" type="script" x="608" y="0" width="32" height="32">
   <properties>
    <property name="when" value="round"/>
   </properties>
  </object>
  <object name="dialogue_id" type="script" x="640" y="0" width="32" height="32">
   <properties>
    <property name="when" value="always"/>
   </properties>
  </object>
  <object name="startitems" type="container" x="704" y="0" width="32" height="32"/>
  <object name="dialogue_id" type="sign" x="768" y="0" width="32" height="32"/>
 </objectgroup>
 <objectgroup name="Spawn">
  <object name="spawn_area_with_spawngroup_id" type="spawn" x="256" y="64" width="192" height="96">
   <properties>
     <property name="spawngroup" value="trainingrat"/>
   </properties>
  </object>
  <object name="spawn_area_inactive_with_npcID_id" type="spawn" x="256" y="192" width="192" height="96">
   <properties>
    <property name="active" value="false"/>
    <property name="quantity" value="2"/>
    <property name="spawngroup" value="tiny_rat"/>
   </properties>
  </object>
 </objectgroup>
 <objectgroup name="Keys">
  <object name="key_area_1_id" type="key" x="544" y="256" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireId" value="andor"/>
    <property name="requireNegation" value="true"/>
    <property name="requireType" value="questProgress"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
  <object name="key_area_2_id" type="key" x="544" y="288" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireId" value="andor"/>
    <property name="requireType" value="questLatestProgress"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
  <object name="key_area_3_id" type="key" x="544" y="320" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireId" value="hair"/>
    <property name="requireType" value="inventoryRemove"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
  <object name="key_area_4_id" type="key" x="544" y="352" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireId" value="hair"/>
    <property name="requireType" value="inventoryKeep"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
  <object name="key_area_5_id" type="key" x="544" y="384" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireId" value="shirt1"/>
    <property name="requireType" value="wear"/>
   </properties>
  </object>
  <object name="key_area_6_id" type="key" x="544" y="416" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireId" value="crit1"/>
    <property name="requireType" value="skillLevel"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
  <object name="key_area_7_id" type="key" x="544" y="448" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireId" value="tiny_rat"/>
    <property name="requireType" value="killedMonster"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
  <object name="key_area_8_id" type="key" x="544" y="480" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireId" value="timer_id"/>
    <property name="requireType" value="timerElapsed"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
  <object name="key_area_9_id" type="key" x="544" y="512" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireId" value="meat"/>
    <property name="requireType" value="usedItem"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
  <object name="key_area_10_id" type="key" x="544" y="544" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireType" value="spentGold"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
  <object name="key_area_11_id" type="key" x="544" y="576" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireType" value="consumedBonemeals"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
  <object name="key_area_12_id" type="key" x="544" y="608" width="224" height="32">
   <properties>
    <property name="phrase" value="dialogue_id"/>
    <property name="requireId" value="ac"/>
    <property name="requireType" value="hasActorCondition"/>
   </properties>
  </object>
 </objectgroup>
 <objectgroup name="Replace">
  <object name="replace_area_id" type="replace" x="192" y="288" width="160" height="160">
   <properties>
    <property name="Above" value="Above_2"/>
    <property name="Ground" value="Ground_2"/>
    <property name="Objects" value="Objects_2"/>
    <property name="Walkable" value="Walkable_2"/>
    <property name="requireId" value="andor"/>
    <property name="requireNegation" value="true"/>
    <property name="requireType" value="questProgress"/>
    <property name="requireValue" value="1"/>
   </properties>
  </object>
 </objectgroup>
</map>
```

</details>


## Worldmaps format.

Worldmaps use a custom XML format. All worldmaps are defined in a single worldmap.xml file located under res/xml/ in the game source.

![ui_icon_map](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/ui_icon_map.png)
The root tag is always "**worldmap**" and is composed of several "**segment**" sub-tags. Each worldmap in ATCS represent a separate "segment". Worldmaps are really best created and edited using ATCS, but masochists can use any text editor.

A "**segment**" tag has the "**x**" and "**y**" properties, both with numerical values, that indicate an offset to the position of all contained areas.

A "**segment**" tag has any number of "**map**" and "**namedarea**" sub-tags.

Each "**map**" tag represent a TMX map that has to appear on the worldmap segment.

- A "**map**" tag ***must ***have the following attributes:
  - "**id**" with a textual value matching the ID of a TMX map.
  - "**x**" with a numerical value corresponding to the X-coordinate of this map within the segment.
  - "**y**" with a numerical value corresponding to the Y-coordinate of this map within the segment.

- A "**map**" tag ***can ***have the following optional attribute:
  - "**area**" with a textual value matching the "**id**" attribute of a "**namedarea**" tag from the same segment. Use this when the map is part of a named area of the map, like a town.


Each "**namedarea**" tag represent a label on the map, like a town name. The label will be displayed centered on the bounding box of all maps that are part of this "**namedarea**".

- A "**namedarea**" ***must ***have the following attributes:
  - "**id**" with a textual value. This is the value that must be referenced in the "**area**" attribute of the "**map**" tags.
  - "**name**" with a textual value. This is the value displayed to the players, so it must be properly capitalized, and can contain spaces.
  - "**type**" with a textual value. It typically contains "settlement" for towns and "other" for the rest, but while the field is mandatory, it is unused by the game engine.


**Full example using all fields.**
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<worldmap>
  <segment id="segmentID" x="43" y="44">
    <map id="blackwater_mountain0" x="43" y="46"/>
    <map area="label_id" id="blackwater_mountain1" x="64" y="44"/>
    <map area="label_id" id="blackwater_mountain10" x="85" y="44"/>
    <namedarea id="label_id" name="Label on map" type="other"/>
  </segment>
</worldmap>
```




## Actor conditions format.

Actor conditions are defined in JSON format. Files containing actor conditions should be named actorconditions_*\<name\>*.json, and placed under res/raw/ in the game source folder. The *\<name\>* can be anything composed of lower case letters, digits, and underscores.

![actor_condition](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/actor_condition.png)
A single file can hold any number of actor conditions, as a list. Even if only one actor condition is defined in a file, it must be contained within a list.

Actor conditions, like all JSON-based content, is best created and edited using ATCS, but a text editor can be used for simple fixes (typos...) or by masochists.

- An **actor condition** ***must*** have the following fields defined:
  - "**id**" with any textual value. I recommend using only lower case letters, digits, and underscores. This will be the technical identifier (ID) for this actor condition. Any other game element that reference an actor condition do so by using the ID.
  - "**name**" with any textual value. This is the display name of the actor condition (in english), as shown to the player in-game. Proper spelling and capitalization are required. When more than one word is used, only the first letter of the first word is capitalized, except for proper nouns (like the Shadow).
  - "**iconID**" with a textual value indicating a sprite ID of the form *<spritesheet ID>*:*<sprite index>*. The *<spritesheet ID>* is the name of the image file as present in res/drawable, without the extension (for example, if the sprite is in res/drawable/actorconditions_1.png, the spritesheet ID is "actorconditions_1"). The sprite index is the number of sprites you have to skip to reach the desired one within the spritesheet, reading left-to-right, starting at 0 for the top-left one. One sprite is generally defined as a 32x32px square within a spritesheet.
  - "**category**" with one of the following textual values: "**spiritual**", "**mental**", "**physical**", or "**blood**". This field describes the kind of affliction caused by this actor condition, and which skills can affect them.


- An **actor condition** ***can*** have the following fields defined:
  - "**isPositive**" with a numerical value of 1 or 0. 0 being the default, you can omit this field entirely when it is the desired value. When the value is 1, it means that the actor condition as a beneficial effect on the afflicted actor. Weak poison is NOT positive, but Shadow Regeneration is.
  - "**isStacking**" with a numerical value of 1 or 0. 0 being the default, you can omit this field entirely when it is the desired value. When the value is 1, it means that the actor condition can be inflicted multiple times simultaneously on the same actor with cumulative effects.
  - "**roundEffect**" with an object value, as defined in the **timed effect** definition below. These effects will be applied to the inflicted actor every round (in battle, or every 6 seconds outside of battle).
  - "**fullRoundEffect**" with an object value, as defined in the **timed effect** definition below. These effects will be applied to the inflicted actor every "full round" (in battle, or every 25 seconds outside of battle).
  - "**abilityEffect**" with an object value, as defined in the **constant effect** definition below. These effects are permanent stats modifiers applied as long as the actor condition is active.


- A **timed effect** ***can*** have the following fields defined:
  - "**visualEffectID**" with one of the following textual values: "**redSplash**", "**blueSwirl**", "**greenSplash**", and "**miss**". Every time the effect will take place, the player will be overlaid by a short animation corresponding to the selected value.
  - "**increaseCurrentHP**" with an object value, as defined in the **min-max values** definition below. These values indicate a range of HP from which a random value will be picked, and added to the current HP of the player. Poisons typically use negative values here.
  - "**increaseCurrentAP**" with an object value, as defined in the **min-max values** definition below. These values indicate a range of AP from which a random value will be picked, and added to the current AP of the player. Useful only for negative values here, as at the beginning of a round, the player will have his AP bar full.


- A **constant effect** ***can*** have the following fields defined:
  - "**increaseAttackChance**" with a numerical value. Will add this value to the total AC of the player while the condition is active.
  - "**increaseAttackDamage**"with an object value, as defined in the **min-max values** definition below. Will add these values to the minimum and maximum AD of the player while the condition is active.
  - "**increaseMaxHP**" with a numerical value. Will add this value to the maximum HP of the player while the condition is active.
  - "**increaseMaxAP**" with a numerical value. Will add this value to the maximum AP of the player while the condition is active.
  - "**increaseMoveCost**" with a numerical value. Will add this value to the amount of AP used by the player to attempt fleeing during combat while the condition is active.
  - "**increaseUseItemCost**" with a numerical value. Will add this value to the amount of AP used by the player to use items (potions, food, etc.) during combat while the condition is active.
  - "**increaseReequipCost**" with a numerical value. Will add this value to the amount of AP used by the player to change gear during combat while the condition is active.
  - "**increaseAttackCost**" with a numerical value. Will add this value to the amount of AP used by the player to attempt one attack while the condition is active.
  - "**increaseCriticalSkill**" with a numerical value. Will add this value to the total CS of the player while the condition is active.
  - "**increaseBlockChance**" with a numerical value. Will add this value to the total BC of the player while the condition is active.
  - "**increaseDamageResistance**" with a numerical value. Will add this value to the total DR of the player while the condition is active.


- **Min-max values** ***must*** have the following fields defined:
  - "**min**" with a numerical value. That's the lowest possible value.
  - "**max**" with a numerical value. That's the highest possible value.


**Full example using all fields.**
```
{
    "id":"ac",
    "iconID":"actorconditions_1:0",
    "name":"Actor Condition",
    "category":"spiritual",
    "isPositive":1,
    "isStacking":1,
    "roundEffect":{
        "visualEffectID":"blueSwirl",
        "increaseCurrentHP":{
            "min":10,
            "max":20
        },
        "increaseCurrentAP":{
            "min":30,
            "max":40
        }
    },
    "fullRoundEffect":{
        "visualEffectID":"blueSwirl",
        "increaseCurrentHP":{
            "min":10,
            "max":20
        },
        "increaseCurrentAP":{
            "min":30,
            "max":40
        }
    },
    "abilityEffect":{
        "increaseAttackChance":50,
        "increaseAttackDamage":{
            "min":30,
            "max":40
        },
        "increaseMaxHP":10,
        "increaseMaxAP":20,
        "increaseMoveCost":90,
        "increaseUseItemCost":110,
        "increaseReequipCost":120,
        "increaseAttackCost":100,
        "increaseCriticalSkill":70,
        "increaseBlockChance":60,
        "increaseDamageResistance":80
    }
}
```





## Dialogues format.

Dialogues are defined in JSON format. Files containing dialogues should be named conversationlist_\<name\>.json, and placed under res/raw/ in the game source folder. The \<name\> can be anything composed of lower case letters, digits, and underscores.

![dialogue](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/dialogue.png)

A single file can hold any number of dialogues, as a list. Even if only one dialogue is defined in a file, it must be contained within a list.
Dialogues, like all JSON-based content, is best created and edited using ATCS, but a text editor can be used for simple fixes (typos...) or by masochists.
For analysis and review, I suggest finding the starting point of a dialogue (either from the NPC editor, or from the Map), and using the "Dialogue tree" tab at the bottom.

Dialogues are the trickiest data type to create and edit for Andor's Trail. The reward & requirement system is at the core of the scripting capabilities of the game engine, and while usually called "dialogues", some never display text, or aren't part of a conversation with a NPC. In this case, they are often called "scripts" (like in the map object type "script").

- A **dialogue** ***must*** have the following field defined:
  - "**id**" with any textual value. I recommend using only lower case letters, digits, and underscores. This will be the technical identifier (ID) for this dialogue. Any other game element that reference a dialogue do so by using the ID.
A **dialogue** ***can*** have the following fields defined:
  - "**message**" with any textual value. This is the text displayed when this dialogue is reached (in english), as shown to the player in-game. Proper spelling and capitalization are required. Multi-line messages are possible by using the "\n" character sequence to indicate a new line. The message can contain "**$playername**", and the game engine will replace this by the player's name in-game.
  - "**switchToNPC**" with a textual value matching the ID of a NPC. This NPC must be present on the map when this dialogue is reached.
  - "**replies**" with a list value containing any number of objects as defined in the **replies** definition below. These define what event or dialogue will come next, depending on player choice and/or requirements on these replies.
  - "**rewards**" with a list value containing any number of objects as defined in the **rewards** definition below. These define what effects reaching this dialogue will have on the player or the world.


- A **reply** ***must*** have the following fields defined:
  - "**text**" with any textual value. This is the text displayed as a selectable reply (in english), as shown to the player in-game. Proper spelling and capitalization are required. Multi-line messages are NOT possible. The text can contain "**$playername**", and the game engine will replace this by the player's name in-game. There is a special case: when the value is "N", no replies are shown to the player, only the "Next" button. This is used to make the NPC continue talking. Use this to put pauses in the text said by the NPC, as long multi-line messages would be hard to follow (or worse, require scrolling) in-game, especially on tiny devices.
  - "**nextPhraseID**" with a textual value matching a dialogue ID or one of the following special values: "**X**", "**F**", "**R**", "**S**". When this reply is selected, the dialogue will continue to the dialogue whose ID is referenced in this field. 
    - When the value is "**X**", the dialogue ends. This is also the case when this field is omitted entirely.
    - When the value is "**F**", the dialogue ends and the player will start fighting the NPC. The player always has the first turn.
    - When the value is "**R**", the dialogue ends and the NPC is removed from the map, as if it was killed, but without loot or splatter.
    - When the value is "**S**", the dialogue ends and the the player starts trading with the active NPC.

  - "**requires**" with a list value containing any number of objects as defined in the **requirements** definition below. All the requirements associated to a given reply must be fulfilled for the reply to be displayed to the player. Think of it as a boolean AND between all requirements of a given reply. If you need to perform a boolean OR, simply create two replies with different requirements, but the same "nextPhraseID".


- A **requirement** ***must*** have the following field defined:
  - "**requireType**" with one of the following textual values: "**questProgress**", "**questLatestProgress**", "**inventoryRemove**", "**inventoryKeep**", "**wear**", "**skillLevel**", "**killedMonster**", "**timerElapsed**", "**usedItem**", "**spentGold**", "**consumedBonemeals**", "**hasActorCondition**", "**factionScore**, "**factionScoreEquals**", "**random**", "**date**", "**dateEquals**", "**time**", "**timeEquals**".

- A **requirement** ***can*** have the following field defined:
    - "**requireID**" with a textual value. As it depends on the "**requireType**" selected, see below for details.
    - "**value**" with a numerical value. As it depends on the "**requireType**" selected, see below for details.
    - "**negate**" with the value true or false. false being the default, this property can be omitted completely in this case. When the value is true, the requirement is fulfilled only when it shouldn't be.
  - When "requireType" is "**questProgress**" or "**questLatestProgress**", the "requireID" field must have a value matching a quest ID, and the "value" must have a value matching a quest step ID. In both case, the player must have reached this quest stage in order to fulfill the requirement. The difference between "questProgress" and "latestQuestProgress" is that the latter also requires that no other quest stage with a greater ID must have been reached (numerical comparison between the stage IDs).

  - When "requireType" is "**inventoryRemove**", "**inventoryKeep**" or "**usedItems**", the "requireID" field must have a value matching an item ID, and the "value" must have a positive numerical value depicting the quantity of the item. In the case of the "**usedItems**" type, the player must have used at least the selected quantity of the selected item type (quaffed X potions, eat Y apples...). In the case of "**inventoryKeep**" and "**inventoryRemove**", the player must have at least the selected quantity of the selected item in his inventory, the difference is that with the latter, these items will be taken from the player. I'd suggest avoiding doing that outside of a dialogue though, or with proper warning.
  - When "requireType" is "**wear**", the "requireID" field must have a value matching an item ID. "value" is not used. To fulfill this, the player must have the selected item equipped.
  - When "requireType" is "**skillLevel**", the "requireID" property must have a value matching a skill ID, as found here: [url]https://github.com/Zukero/andors-trail/blob/master/AndorsTrail/src/com/gpl/rpg/AndorsTrail/model/ability/SkillCollection.java[/url]. The "value" field must have a numerical value that indicate the level of the skill that is needed to fulfill the requirement.
  - When "requireType" is "**killedMonster**", the "requireID" field must have a value matching a NPC ID, and the "value" field must be a numerical value that indicate the number of the given NPC that the player must have killed.
  - When "requireType" is "**timerElapsed**", the "requireID" field must be a textual value matching the ID of a timer started by a dialogue. The "value" must be a numerical value indicating the number of game rounds that must have passed since the timer was last started.
  - When "requireType" is "**factionScore**" or "**factionScoreEquals**", the "requireID" field must be a textual value matching the ID of a faction whose score was changed by a dialogue using the "alignmentChange" reward. The "value" must be a numerical value (positive or negative) indicating the minimum or exact score the player must have for this faction to fulfill this requirement.
  - When "requireType" is "**spentGold**", the "requireID" field is unused, and the "value" must be a numerical value indicating the total amount of gold that the player must have spent overall.
  - When "requireType" is "**consumedBonemeals**", the "requireID" field is unused, and the "value" must be a numerical value indicating the total amount of bonemeal potions (all kind of bonemeals, including Lodar's) that the player must have consumed overall.
  - When "requireType" is "**hasActorConditions**", the "requireID" field must have a value matching an actor condition ID. The "value" property is unused. The requirement is fulfilled when the player is afflicted by the selected actor condition.
  - When "requireType" is "**random**", the "requireID" field must be a chance value. The requirement is fulfilled with this chance value. Use "100" for a sure drop, "50" for 50% chance, "0.1" for extraordinary items, and "0.01" for legendary items. Instead of percent values you may also use fractions like "1/3". 
  - When "requireType" is "**date**" or "**dateEquals**", the "requireID" field must be one of these format constants: YYYYMMDD, YYYYMM, YYYY, MMDD, MM, DD. The "value" property is a an integer in the length of the used format.
  - When "requireType" is "**time**" or "**timeEquals**", the "requireID" field must be one of these format constants: HHMMSS, HHMM, HH, MMSS, MM, SS. The "value" property is a an integer in the length of the used format.


- A **reward** ***must*** have the following fields defined:
  - "rewardType" with one of the following values: "**questProgress**", "**removeQuestProgress**", "**dropList**", "**skillIncrease**", 
    "**actorCondition**", "**actorConditionImmunity**", "**alignmentChange**", "**alignmentSet**", "**alignmentToReg1**", "**alignmentFromReg1**",
    "**alignmentAdd**", "**alignmentSub**", "**alignmentMult**", "**alignmentDiv**", "**giveItem**", "**createTimer**", "**spawnAll**", 
    "**removeSpawnArea**", "**deactivateSpawnArea**", "**activateMapObjectGroup**", "**deactivateMapObjectGroup**", "**changeMapFilter**",
    "**mapchange**". Explanation for all rewards type, and associated constraints on the other fields of a reply definition is below.

  - "rewardID" with a textual value generally matching the ID of a game object. As it depends on the "**rewardType**" selected, see below for details.

- A **reward** ***can*** have the following fields defined:
  - "**value**" with a numerical value. As it depends on the "**rewardType**" selected, see below for details.
  - "**mapName**" with a textual value matching a map ID. As it depends on the "**rewardType**" selected, see below for details.
  - When "rewardType" is "**questProgress**" or "**removeQuestProgress**", the "rewardID" field must match a quest's ID, and the "value" field must match a step ID that is defined in the selected quest. The "mapName" field is unused. "**questProgress**" will grant that progress, while "**removeQuestProgress**" will remove that progress from the player. The latter should be used only for *hidden* quests, as removing entries from the player's quest log can be awkward.
  - When "rewardType" is "**dropList**", the "rewardID" field must match a droplist's ID, and the "value" and "mapName" fields are unused.
When granted this reward, the player will receive all the items from the droplists, taking the variability of the droplist into account (some may have random quantities, or even random presence). 
  - When "rewardType" is "**skillIncrease**", the "rewardID" field must match a skill ID, as found here: [url]https://github.com/Zukero/andors-trail/blob/master/AndorsTrail/src/com/gpl/rpg/AndorsTrail/model/ability/SkillCollection.java[/url]. The "value" and "mapName" fields are unused. 
When granted this reward, the player will see this skill's level incremented by one.
  - When "rewardType" is "**actorCondition**" or "**actorConditionImmunity**", the "rewardID" field must match an actor condition's ID, and the "value" field will indicate the number of rounds this actor conditions last. The "mapName" field is unused. 
  In the case of "**actorCondition**", when granted this reward, the player will be afflicted by the actor condition for the selected number of rounds. In the case of "**actorConditionImmunity**", when granted this reward, the player will not be afflicted by the actor condition for the selected number of rounds. 
  Special "value":
	- 999 to indicate that this actor condition lasts forever (rotworms anyone?). 
	- 998 to indicate that this actor condition lasts until next time you sleep.
	- -99 means the player will be cleared of all active instances of this actor condition.
  - When the "rewardType" is "**alignmentChange**" or "**alignmentSet**", the "rewardID" field must be an arbitrary textual value that will serve as the faction ID to use in "factionScore" requirements or NPCs' "faction" field, and the "value" field must be an integer indicating the score. In the case of "alignmentChange", the score set in "value" is added to the current score for that faction ID, while "alignmentSet" sets the value as the new score, regardless of the previous value.
  - When "rewardType" is "**giveItem**", the "rewardID" field must match an item's ID, and the "value" field indicate the quantity. The "mapName" field is unused. When granted this reward, the player will receive the selected quantity of the selected item. This is quite similar to the "droplist" reward type, but it is simpler (no variability possible), and avoids the burden of creating a droplist when you just want to give one or two items, or some gold.
  - When "rewardType" is "**createTimer**", the "rewardID" field must contain any textual value that will be this timer's ID. The "value" and "mapName" fields are unused. When granted this reward, the game will keep note of the game time (in game rounds elapsed since you started the game), and use this for comparison in requirements. Every time you *create* a timer, any previous note with the same timer ID will be overwritten.
  - When "rewardType" is "**spawnAll**", "**removeSpawnArea**", or "**deactivateSpawnArea**", the "mapName" field must match a map's ID, and the "rewardID" field must match a spawn area's ID within the selected map. The "value" field is unused. In the case of "**spawnAll**", when granted this reward, this spawn area will be activated (if it was inactive), and all included NPCs will be spawned immediately. In the case of "**removeSpawnArea**", when granted this reward, this spawn area will be deactivated, and all included NPCs will be removed immediately. In the case of "**deactivateSpawnArea**", when granted this reward, this spawn area will be deactivated, but all included NPCs will remain on the map until killed or removed by a dialogue reward.
  - When "rewardType" is "**activateMapObjectGroup**", or "**deactivateMapObjectGroup**", the "mapName" field must match a map's ID, and the "rewardID" field must match an object group's ID within the selected map. The "value" field is unused. In the case of "**activateMapObjectGroup**", when granted this reward, all map objects (except spawn areas) within this object group will be made active. In the case of "**deactivateMapObjectGroup**", when granted this reward, all map objects (except spawn areas) within this object group will be made inactive (they have no effect on the game anymore).  :!: Beware though, as deactivating an object group containing a replace area that has already been triggered will NOT revert the map to its previous look.
  - When "rewardType" is "**changeMapFilter**", the "mapName" field must match a map's ID, and the "rewardID" field must match a color filter's ID, as found here: [url]https://github.com/AndorsTrailRelease/ATCS/blob/master/src/com/gpl/rpg/atcontentstudio/model/maps/TMXMap.java[/url]. The "value" field is unused. When granted this reward, the selected map will have its "colorFilter" property changed to the value of the "rewardID" field. See the post about maps to know the effect of the different available color filters.
  - When "rewardType" is "**mapchange**", the "mapName" field must match a map's ID, and the "rewardID" field must match a mapchange target within the selected map. The "value" field is unused.


**The special case of selectors**
In a dialogue, notably as the starting point of a dialogue, you'll often want to use branches, different paths. A NPC may first greet you with "I don't have time", but later, depending on your choices, may change that to "Hello friend!" or "How dare you? After all you did to us!".
To do so, we use a special kind of dialogue we call a **selector**.

To create a selector, simply create a dialogue with no "**message**" field, and add a bunch of replies with no text (use "N" as the "text" field of the reply) with different requirements.
The game engine will evaluate the requirements of each reply, one after the other, in the order they are defined in the JSON file. The first reply for which all requirements are fulfilled is selected, and the script goes on to the dialogue indicated by the "nextPhraseID" field of the reply. It is often useful to also have the last reply with no requirements, to be used as the default.

**Full example using all fields.**
<details>
<summary>Example dialogue - click to open</summary>

```
{
    "id":"dialogue_id",
    "message":"This is line one.\nThis is line two.",
    "switchToNPC":"tiny_rat",
    "replies":[
        {
            "text":"N",
            "nextPhraseID":"mikhail_rats_start2a"
        },
        {
            "text":"Reply. NPC replies too.",
            "nextPhraseID":"mikhail_rats_start2a"
        },
        {
            "text":"Reply. Dialgoue ends.",
            "nextPhraseID":"X"
        },
        {
            "text":"Reply. Let's fight.",
            "nextPhraseID":"F"
        },
        {
            "text":"Reply. NPC disappears.",
            "nextPhraseID":"R"
        },
        {
            "text":"Reply. Starts trading with NPC.",
            "nextPhraseID":"S",
            "requires":[
                {
                    "requireType":"questProgress",
                    "requireID":"andor",
                    "value":1,
                    "negate":true
                },
                {
                    "requireType":"questLatestProgress",
                    "requireID":"andor",
                    "value":1
                },
                {
                    "requireType":"inventoryRemove",
                    "requireID":"hair",
                    "value":1
                },
                {
                    "requireType":"inventoryKeep",
                    "requireID":"hair",
                    "value":1
                },
                {
                    "requireType":"wear",
                    "requireID":"shirt1"
                },
                {
                    "requireType":"skillLevel",
                    "requireID":"crit1",
                    "value":1
                },
                {
                    "requireType":"killedMonster",
                    "requireID":"tiny_rat",
                    "value":10
                },
                {
                    "requireType":"timerElapsed",
                    "requireID":"aze",
                    "value":10
                },
                {
                    "requireType":"usedItem",
                    "requireID":"meat",
                    "value":10
                },
                {
                    "requireType":"spentGold",
                    "value":10
                },
                {
                    "requireType":"consumedBonemeals",
                    "value":10
                },
                {
                    "requireType":"hasActorCondition",
                    "requireID":"chaotic_grip"
                },
                {
                    "requireType":"random",
                    "value":"20"
                },
                {
                    "requireType":"date",
                    "requireID":"YYYYMMDD",
                    "value":20241231
                },
                {
                    "requireType":"dateEquals",
                    "requireID":"MMDD",
                    "value":1231
                },
                {
                    "requireType":"time",
                    "requireID":"HHMMSS",
                    "value":201500
                },
                {
                    "requireType":"timeEquals",
                    "requireID":"HHMM",
                    "value":0830
                }
            ]
        }
    ],
    "rewards":[
        {
            "rewardType":"questProgress",
            "rewardID":"andor",
            "value":1
        },
        {
            "rewardType":"removeQuestProgress",
            "rewardID":"andor",
            "value":1
        },
        {
            "rewardType":"dropList",
            "rewardID":"startitems"
        },
        {
            "rewardType":"skillIncrease",
            "rewardID":"crit1"
        },
        {
            "rewardType":"actorCondition",
            "rewardID":"chaotic_grip",
            "value":1
        },
        {
            "rewardType":"alignmentChange",
            "rewardID":"faction_id",
            "value":1
        },
        {
            "rewardType":"giveItem",
            "rewardID":"hair",
            "value":1
        },
        {
            "rewardType":"createTimer",
            "rewardID":"timer_id"
        },
        {
            "rewardType":"spawnAll",
            "rewardID":"spawn_area_id",
            "mapName":"blackwater_mountain0"
        },
        {
            "rewardType":"removeSpawnArea",
            "rewardID":"spawn_area_id",
            "mapName":"blackwater_mountain0"
        },
        {
            "rewardType":"deactivateSpawnArea",
            "rewardID":"spawn_area_id",
            "mapName":"blackwater_mountain0"
        },
        {
            "rewardType":"activateMapObjectGroup",
            "rewardID":"object_group_id",
            "mapName":"blackwater_mountain0"
        },
        {
            "rewardType":"deactivateMapObjectGroup",
            "rewardID":"object_group_id",
            "mapName":"blackwater_mountain0"
        },
        {
            "rewardType":"changeMapFilter",
            "rewardID":"black20",
            "mapName":"blackwater_mountain0"
        },
        {
            "rewardType":"mapchange",
            "rewardID":"north",
            "mapName":"blackwater_mountain0"
        }
    ]
}
```

</details>



## Droplists format.

Droplists are defined in JSON format. Files containing droplists should be named droplists_*\<name\>*.json, and placed under res/raw/ in the game source folder. The *\<name\>* can be anything composed of lower case letters, digits, and underscores.

![ui_icon_equipment](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/ui_icon_equipment.png)
A single file can hold any number of droplists, as a list. Even if only one droplist is defined in a file, it must be contained within a list.
Droplists, like all JSON-based content, is best created and edited using ATCS, but a text editor can be used for simple fixes (typos...) or by masochists.

[list=]A **droplist** ***must*** have the following fields defined:
  - "**id**" with any textual value. I recommend using only lower case letters, digits, and underscores. This will be the technical identifier (ID) for this droplist. Any other game element that reference a droplist do so by using the ID.
  - "**items**" with a list value containing any number of objects as defined in the dropped items definition below. These are the items that can be dropped as part of this droplist, along with individual quantity range and drop chance.


- A **dropped item** ***must*** have the following fields defined:
  - "**itemID**" with a textual value matching an item's ID.
  - "**chance**" with a numerical value (can be decimal). It defines the percentage of chance that this item will be dropped in this droplist. Use "100" for a sure drop, "50" for 50% chance, "0.1" for extraordinary items, and "0.01" for legendary items. Instead of percent values you may also use fractions like "1/3". 
  - "**quantity**" with an object value, as defined in the **min-max values** definition below. The quantity actually dropped will be picked at random within this range each time this droplist is dropped.


- **Min-max values** ***must*** have the following fields defined:
  - "**min**" with a numerical value. That's the lowest possible value.
  - "**max**" with a numerical value. That's the highest possible value.

**Full example using all fields.**
```
{
    "id":"droplist_id",
    "items":[
        {
            "itemID":"hair",
            "chance":"100",
            "quantity":{
                "min":1,
                "max":1
            }
        },
        {
            "itemID":"ring_shadow0",
            "chance":"0.001",
            "quantity":{
                "min":1,
                "max":1
            }
        },
        {
            "itemID":"gold",
            "chance":"50",
            "quantity":{
                "min":0,
                "max":5000
            }
        },
        {
            "itemID":"health_minor",
            "chance":"100",
            "quantity":{
                "min":2,
                "max":5
            }
        }
    ]
}
```





## Items format.

Items are defined in JSON format. Files containing items should be named itemlist_\<name\>.json, and placed under res/raw/ in the game source folder. The \<name\> can be anything composed of lower case letters, digits, and underscores.

![item](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/item.png)

A single file can hold any number of items , as a list. Even if only one item is defined in a file, it must be contained within a list.
Items, like all JSON-based content, is best created and edited using ATCS, but a text editor can be used for simple fixes (typos...) or by masochists.

- An **item** ***must*** have the following fields defined:
  - "**id**" with any textual value. I recommend using only lower case letters, digits, and underscores. This will be the technical identifier (ID) for this item. Any other game element that reference an item do so by using the ID.
  - "**name**" with any textual value. This is the display name of the item (in english), as shown to the player in-game. Proper spelling and capitalization are required. When more than one word is used, only the first letter of the first word is capitalized, except for proper nouns (like the Shadow). The name can contain "**$playername**", and the game engine will replace this by the player's name in-game.
  - "**iconID**" with a textual value indicating a sprite ID of the form <spritesheet ID>:<sprite index>. The <spritesheet ID> is the name of the image file as present in res/drawable, without the extension (for example, if the sprite is in res/drawable/items_armours.png, the spritesheet ID is "items_armours"). The sprite index is the number of sprites you have to skip to reach the desired one within the spritesheet, reading left-to-right, starting at 0 for the top-left one. One sprite is generally defined as a 32x32px square within a spritesheet.


- An **item** ***can*** have the following fields defined:
  - "**displaytype**" with one of the following values: "**ordinary**", "**quest**", "**extraordinary**", "**legendary**", "**rare**". All but the ordinary will be displayed with a colored halo in-game, and quest items cannot be sold.
  - "**hasManualPrice**" with the value 1 or 0. 0 being the default, this field can be omitted entirely when this is the case. When set to 1, the base price of this item has to de defined using the "**baseMarketCost**" field.
  - "**baseMarketCost**" with a numerical positive integer value. This is the base cost of the item, when "**hasManualPrice**" is set to 1. By default, players will be able to buy this item for 115% of this value, and sell it for 85% of this value. If this is set to 0, this item cannot be sold.
  - "**category**" with a textual value matching an item category's ID. The selected category will contain the information that tells whether this item can be worn (equipped, and the associated slot) or consumed (drank, eaten...). This will in turn have an effect on other fields of the item definition.
  - "**description**" with any textual value. This is the displayed description of the item (in english), as shown to the player in-game in the item info pop-up. Proper spelling and capitalization are required. 
  - "useEffect" with an object value, as defined in the **timed effect** definition below. This field can exist only when the selected "**category**" is one for consumables, and the described effect will occur upon consumption of the item by the player.
  - "equipEffect" with an object value, as defined in the **equip effect**" definition below. This field can exist only when the selected "**category**" is one for equipment, and the described effect will by applied to player upon equipping that item.
  - "hitEffect" with an object value, as defined in the **hit effect** definition below. This field can exist only when the selected "**category**" is one for equipment, and the described effect will occur when the player successfully hits an enemy while this item is equipped.
  - "killEffect" with an object value, as defined in the **timed effect** definition below. This field can exist only when the selected "**category**" is one for equipment, and the described effect will occur when the player kills an enemy while this item is equipped.


- A **timed effect** ***can*** have the following fields defined:
  - "**increaseCurrentHP**" with an object value, as defined in the **min-max values** definition below. These values indicate a range of HP from which a random value will be picked, and added to the current HP of the player. This cannot make the current HP exceed the max HP of the player.
  - "**increaseCurrentAP**" with an object value, as defined in the **min-max values** definition below. These values indicate a range of AP from which a random value will be picked, and added to the current AP of the player. This cannot make the current AP exceed the max AP of the player.
  - "**conditionsSource**" with a list value containing any number of objects as defined in the **timed actor condition** definition below. All these actor conditions (with associated magnitude and duration) have a chance to be applied to the player when this **timed effect** is triggered.


- A **hit effect** has the same definition as a **timed effect**, with the addition of the following ***optional*** field:
  - "**conditionsTarget**" with a list value containing any number of objects as defined in the **timed actor condition** definition below. All these actor conditions (with associated magnitude and duration) have a chance to be applied to the targeted enemy when this **hit effect** is triggered.


- A **timed actor condition** ***must*** have the following fields defined:
  - "**condition**" with a textual value matching an actor condition's ID. This is the actor condition that will potentially affect the target (player or enemy).
  - "**chance**" with a textual representation of a numerical value. This is the percentage of chance that the actor condition will be applied. "100" means that the condition will always be applied.


- A **timed actor condition** ***can*** have the following fields defined:
  - "**duration**" with a numerical value. This is the number of rounds the effect will last. The value 999 can be used to mean that the effect lasts forever, and can only be removed by *clearing* this actor condition, but this is the default value, and this field can be omitted entirely when this is the case.
  - "**magnitude**"  with a numerical integer value. This is a multiplier used to amplify the effect of the base actor condition. The value -99 can be used to mean that this effect will *clear* the target from this actor condition, but this is the default value, and this field can be omitted entirely when this is the case.


- An **equip effect** ***can*** have the following fields defined:
  - "**increaseAttackDamage**" with an object value, as defined in the **min-max values** definition below. These values indicate the amount of AD to be added the the minimum and maximum AD of the player.
  - "**increaseMaxHP**" with a numerical integer value. This value will be added to the maximum HP of the player.
  - "**increaseMaxAP**" with a numerical integer value. This value will be added to the maximum AP of the player.
  - "**increaseMoveCost**" with a numerical integer value. This value will be added to the AP cost of moving (fleeing) of the player.
  - "**increaseUseItemCost**" with a numerical integer value. This value will be added to the AP cost of using items (potions...) of the player.
  - "**increaseReequipCost**" with a numerical integer value. This value will be added to the AP cost of changing equipment of the player.
  - "**increaseAttackCost**" with a numerical integer value. This value will be added to the AP cost of attempting an attack of the player.
  - "**increaseAttackChance**" with a numerical integer value. This value will be added to the AC of the player.
  - "**increaseCriticalSkill**" with a numerical integer value. This value will be added to the CS of the player.
  - "**increaseBlockChance**" with a numerical integer value. This value will be added to the BC of the player.
  - "**increaseDamageResistance**" with a numerical integer value. This value will be added to the DR of the player.
  - "**setCriticalMultiplier**" with a numerical float value. The player's CM is always the main-hand weapon's CM (except for Dual-Wield, where it is the best available CM once eventual maluses have been applied to the off-hand weapon).
  - "**addedConditions**" with a list value containing any number of objects as defined in the **equipment actor condition** definition below. All these actor conditions will affect the player as long as this item is equipped.


- An **equipment actor condition** ***must*** have the following field defined:
  - "**condition**" with a textual value matching an actor condition's ID. This is the actor condition that will affect the player.


- An **equipment actor condition** ***can*** have the following field defined:
  - "**magnitude**"  with a numerical integer value. This is a multiplier used to amplify the effect of the base actor condition. The value -99 can be used to mean that this effect will *clear* the target from this actor condition. The default value is 1, and this field can be omitted entirely when this is the case.


- **Min-max values** ***must*** have the following fields defined:
  - "**min**" with a numerical value. That's the lowest possible value.
  - "**max**" with a numerical value. That's the highest possible value.


**Full example using almost all fields.** The "**useEffect**" field dedicated to usable items, and missing in this example, has the same syntax as the "**killEffect**" field dedicated to equipment, as depicted below.
<details>
<summary>Example items - click to open</summary>

```
{
    "id":"item_id",
    "iconID":"items_armours:0",
    "name":"Item name",
    "displaytype":"ordinary",
    "hasManualPrice":1,
    "baseMarketCost":2,
    "category":"buckler",
    "description":"This is a description.",
    "equipEffect":{
        "increaseAttackDamage":{
            "min":10,
            "max":20
        },
        "increaseMaxHP":30,
        "increaseMaxAP":40,
        "increaseMoveCost":100,
        "increaseUseItemCost":110,
        "increaseReequipCost":120,
        "increaseAttackCost":130,
        "increaseAttackChance":50,
        "increaseCriticalSkill":70,
        "increaseBlockChance":60,
        "increaseDamageResistance":90,
        "setCriticalMultiplier":80.0,
        "addedConditions":[
            {
                "condition":"ac",
                "magnitude":1
            }
        ]
    },
    "hitEffect":{
        "increaseCurrentHP":{
            "min":10,
            "max":20
        },
        "increaseCurrentAP":{
            "min":30,
            "max":40
        },
        "conditionsSource":[
            {
                "condition":"ac",
                "duration":1,
                "chance":"50"
            },
            {
                "condition":"ac",
                "magnitude":-99,
                "chance":"50"
            }
        ],
        "conditionsTarget":[
            {
                "condition":"ac",
                "magnitude":2,
                "duration":2,
                "chance":"100"
            }
        ]
    },
    "killEffect":{
        "increaseCurrentHP":{
            "min":10,
            "max":20
        },
        "increaseCurrentAP":{
            "min":30,
            "max":40
        },
        "conditionsSource":[
            {
                "condition":"ac",
                "magnitude":3,
                "duration":3,
                "chance":"100"
            }
        ]
    }
}
```

</details>



## Item categories format.

Item categories are defined in JSON format. Files containing item categories should be named itemcategories_\<name\>.json, and placed under res/raw/ in the game source folder. The \<name\> can be anything composed of lower case letters, digits, and underscores.

![equip_shield](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/equip_shield.png)

A single file can hold any number of item categories, as a list. Even if only one item category is defined in a file, it must be contained within a list.
Item categories, like all JSON-based content, is best created and edited using ATCS, but a text editor can be used for simple fixes (typos...) or by masochists.

- An **item category** ***must*** have the following fields defined:
  - "**id**" with any textual value. I recommend using only lower case letters, digits, and underscores. This will be the technical identifier (ID) for this item category. Any other game element that reference an item category do so by using the ID.
  - "**name**" with any textual value. This is the display name of the item category (in english), as shown to the player in-game. Proper spelling and capitalization are required. When more than one word is used, only the first letter of the first word is capitalized, except for proper nouns (like the Shadow).


- An **item category** ***can*** have the following fields defined:
  - "**actionType**" with one of the following values: "**none**", "**use**", or "**equip**". When the value is "**none**", the player can neither use nor equip items belonging to this category. This is the default value, and this field can be omitted entirely when this is the case. When the value is "**use**", the player can *consume* items belonging to this category. When the value is "**equip**", the player can equip the items belonging to this category in the equipment slot defined using the "**inventorySlot**" field for this item category. The "**inventorySlot**" field is mandatory when the value "**equip**" is used.
  - "**size**" with one of the following values:  "**none**", "**light**", "**std**", or "**large**". This indicate the size of the items belonging to this category, and is used by the skill system to provide benefits to using one size of equipment or another. "**none**" is the default value, and this field can be omitted entirely when this is the case. 
  - "**inventorySlot**" with one of the following values: "**weapon**", "**shield**", "**head**", "**body**", "**hand**", "**feet**", "**neck**", "**leftring**", or "**rightring**". This field is unused when "**actionType**" is not set to "**equip**", but is mandatory when it is. The values should be pretty self-explanatory, and define what slot of the player's equipment can be used by items belonging to this category. "**leftring**" and "**rightring**" are completely equivalent to each other in an item category definition. They are both present because the same *Enum* as the one defining equipment slots is used.


**Full example using all fields.**
```
{
    "id":"item_category_id",
    "name":"Item category",
    "actionType":"equip",
    "size":"large",
    "inventorySlot":"shield"
}
```






## NPCs format.

NPCs are defined in JSON format. Files containing NPCs should be named monsterlist_\<name\>.json, and placed under res/raw/ in the game source folder. The \<name\> can be anything composed of lower case letters, digits, and underscores.

![npc](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/npc.png)

A single file can hold any number of NPCs, as a list. Even if only one NPC is defined in a file, it must be contained within a list.
NPCs, like all JSON-based content, is best created and edited using ATCS, but a text editor can be used for simple fixes (typos...) or by masochists.

- A **NPC** ***must*** have the following fields defined:
  - "**id**" with any textual value. I recommend using only lower case letters, digits, and underscores. This will be the technical identifier (ID) for this NPC. Any other game element that reference a NPC do so by using the ID.
  - "**name**" with any textual value. This is the display name of the NPC (in english), as shown to the player in-game. Proper spelling and capitalization are required. When more than one word is used, only the first letter of the first word is capitalized, except for proper nouns (like the Shadow).
  - "**iconID**" with a textual value indicating a sprite ID of the form <spritesheet ID>:<sprite index>. The <spritesheet ID> is the name of the image file as present in res/drawable, without the extension (for example, if the sprite is in res/drawable/monsters_armor1.png, the spritesheet ID is "monsters_armor1"). The sprite index is the number of sprites you have to skip to reach the desired one within the spritesheet, reading left-to-right, starting at 0 for the top-left one. One sprite is generally defined as a 32x32px square within a spritesheet.


- A **NPC** ***can*** have the following fields defined:
  - "**maxHP**" with a numerical integer value. This is the number of HP this type of NPC will have when spawned. The default value is 1.
  - "**maxAP**" with a numerical integer value. This is the number of AP this type of NPC will have at the beginning of a round. The default value is 10.
  - "**moveCost**" with a numerical integer value. This is the AP cost of moving for this type of NPC. It has an impact on the movement speed of the NPC even out of combat. The default value is 10.
  - "**unique**" with the value 1 or 0. When it is 1, the NPC is considered unique, and will not be respawned upon death or removal. The same unique NPC can appear multiple times, if it is referenced by several spawn areas, or with a quantity >1 in a single spawn area. The default value is 0.
  - "**monsterClass**" with one of the following textual values: "**humanoid**", "**insect**", "**demon**", "**construct**", "**animal**", "**giant**", "**undead**", "**reptile**", "**ghost**". The selected monster class will imply the type of splatter to use when killed (dirt, blood or dust), and whether the NPC is immune to critical hits ("**demon**", "**construct**", and "**ghost**") or not. The default value is "**humanoid**".
  - "**movementAggressionType**" with one of the following values: "**none**", "**helpOthers**", "**protectSpawn**", "**wholeMap**". This will affect if and when the NPC will track down the player. With the default value "**none**", the NPC will keep roaming its spawn area, and will never track down the player. With the value "**helpOthers**", the NPC will track down the player if the player is fighting another NPC from the same spawn area. With the value "**protectSpawn**", the NPC will track down the player as long as the player is in the NPC's spawn area. With "**wholeMap**", the NPC will track down the player anywhere on the map, all the time, except if no path can be found to reach the player.
  - "**attackDamage**" with an object value, as defined in the **min-max values** definition below. These values indicate the minimum and maximum AD of the NPC.
  - "**spawnGroup**" with any textual value. This value must match the "**spawnGroup**" of other NPCs so that a single spawn area can randomly spawn any type of NPC among all those that have the same spawn group.
  - "**faction**" with any textual value. This is related to the "**alignmentChange**" and "**alignmentSet**" dialogue rewards types, and the "**factionScore**" dialogue requirements type. When a player's score for this "faction" is negative, the NPC will be aggressive: no dialogue is possible, and aggressive movement can occur.
  - "**phraseID**" with a textual value matching a dialogue's ID. When this is null, the NPC is always aggressive. To prevent the player from fighting a NPC, the NPC must have a "**phraseID**" set. Talkative NPC can become aggressive as part of a dialogue, and once the fight starts, there is no way to talk to the NPC again, only fight.
  - "**droplistID**" with a textual value matching a droplist's ID. For aggressive monsters, this droplist describes the loot they can drop, while for a talkative NPC, it can describe the shop list, or the droplist, depending on whether the dialogue can lead to fighting or trading. Droplists dedicated to trading should always have 100% chance to drop all their items, though the quantities can be random.
  - "**attackCost**" with a numerical integer value. This is the AP cost of attacking for this type of NPC. The default value is 10.
  - "**attackChance**" with a numerical integer value. This is the AC for this type of NPC. The default value is 0.
  - "**criticalSkill**" with a numerical integer value. This is the CS for this type of NPC. The default value is 0.
  - "**criticalMultiplier**" with a numerical float value. This is the CM for this type of NPC. The default value is 0.
  - "**blockChance**" with a numerical integer value. This is the BC for this type of NPC. The default value is 0.
  - "**damageResistance**" with a numerical integer value. This is the DR for this type of NPC. The default value is 0.
  - "**hitEffect**" with an object value, as defined in the **hit effect** definition below. This defines the effect that are triggered when the NPC successfully hits the player.


- A **hit effect** ***can*** have the following fields defined:
  - "**increaseCurrentHP**" with an object value, as defined in the **min-max values** definition below. These values indicate a range of HP from which a random value will be picked, and added to the current HP of the NPC. This cannot make the current HP exceed the max HP of the NPC.
  - "**increaseCurrentAP**" with an object value, as defined in the **min-max values** definition below. These values indicate a range of AP from which a random value will be picked, and added to the current AP of the NPC. This cannot make the current AP exceed the max AP of the NPC.
  - "**conditionsSource**" with a list value containing any number of objects as defined in the **timed actor condition** definition below. All these actor conditions (with associated magnitude and duration) have a chance to be applied to the NPC when this **hit effect** is triggered.
  - "**conditionsTarget**" with a list value containing any number of objects as defined in the **timed actor condition** definition below. All these actor conditions (with associated magnitude and duration) have a chance to be applied to the player when this **hit effect** is triggered.


- A **timed actor condition** ***must*** have the following fields defined:
  - "**condition**" with a textual value matching an actor condition's ID. This is the actor condition that will potentially affect the target (player or enemy).
  - "**chance**" with a textual representation of a numerical value. This is the percentage of chance that the actor condition will be applied. "100" means that the condition will always be applied.


- A **timed actor condition** ***can*** have the following fields defined:
  - "**duration**" with a numerical value. This is the number of rounds the effect will last. The value 999 can be used to mean that the effect lasts forever, and can only be removed by *clearing* this actor condition, but this is the default value, and this field can be omitted entirely when this is the case.
  - "**magnitude**"  with a numerical integer value. This is a multiplier used to amplify the effect of the base actor condition. The value -99 can be used to mean that this effect will *clear* the target from this actor condition, but this is the default value, and this field can be omitted entirely when this is the case.


- **Min-max values** ***must*** have the following fields defined:
  - "**min**" with a numerical value. That's the lowest possible value.
  - "**max**" with a numerical value. That's the highest possible value.


**Full example using all fields.**
```
{
    "id":"npc_id",
    "name":"NPC Name",
    "iconID":"monsters_armor1:0",
    "maxHP":11,
    "maxAP":110,
    "moveCost":110,
    "unique":1,
    "monsterClass":"demon",
    "movementAggressionType":"wholeMap",
    "attackDamage":{
        "min":10,
        "max":10
    },
    "spawnGroup":"npc_spawngroup",
    "faction":"npc_faction",
    "phraseID":"dialogue_id",
    "droplistID":"droplist_id",
    "attackCost":110,
    "attackChance":10,
    "criticalSkill":10,
    "criticalMultiplier":10.0,
    "blockChance":10,
    "damageResistance":10,
    "hitEffect":{
        "increaseCurrentHP":{
            "min":10,
            "max":10
        },
        "increaseCurrentAP":{
            "min":10,
            "max":10
        },
        "conditionsSource":[
            {
                "condition":"ac",
                "magnitude":2,
                "duration":2,
                "chance":"100"
            }
        ],
        "conditionsTarget":[
            {
                "condition":"ac",
                "magnitude":11,
                "duration":10,
                "chance":"10"
            }
        ]
    }
}
```





## Quests format.

Quests are defined in JSON format. Files containing quests should be named questlist_\<name\>.json, and placed under res/raw/ in the game source folder. The \<name\> can be anything composed of lower case letters, digits, and underscores.

![ui_icon_quest](https://raw.githubusercontent.com/AndorsTrailRelease/ATCS/master/src/com/gpl/rpg/atcontentstudio/img/ui_icon_quest.png)

A single file can hold any number of quests, as a list. Even if only one quest is defined in a file, it must be contained within a list.
Quests, like all JSON-based content, is best created and edited using ATCS, but a text editor can be used for simple fixes (typos...) or by masochists.


- A **quest** ***must*** have the following fields defined:
  - "**id**" with any textual value. I recommend using only lower case letters, digits, and underscores. This will be the technical identifier (ID) for this quest. Any other game element that reference an item do so by using the ID.
  - "**name**" with any textual value. This is the display name of the quest (in english), as shown to the player in-game. Proper spelling and capitalization are required. 
  - "**stages**"  with a list value containing any number of objects as defined in the **quest stage** definition below. These are different steps of the quest a player can reach, and each one represents a possible entry in the quest log of the player.


- A **quest** ***can*** have the following field defined:
  - "**showInLog**" with a numerical value of 1 or 0. 0 being the default, this field can be omitted entirely when this is the case. When this is set to 1 (the most common case), this quest is visible to the player: entries will appear in the in-game quest log, and the notification of quest progress will be shown in dialogues.


- A **quest stage** ***must*** have the following fields defined:
  - "**progress**" with a numerical integer value. This is the ID of this quest stage, as will be referenced in the rewards & requirements system of maps and dialogues. It must be unique within the quest. The fact that it is a numerical value does not mean that there must be some order. You *could* number your quest's stages randomly. However, the best practice is to use growing numbers, and mainly multiples of 10, so that it is easier to insert quest stages later should this quest be altered for a bug fix or some design changes in the future. Using stage 20 and 25 (for example) to indicate two alternatives at the same point in a quest is a common convention too.
  - "**logText**" with any textual value. This is the text displayed in the quest log when this stage has been reached (in english). Proper spelling and capitalization are required. Multi-line messages are possible by using the "\n" character sequence to indicate a new line.


- A **quest stage** ***can*** have the following fields defined:
  - "**rewardExperience**" with a numerical positive integer value. When the player first reaches this quest stage, he will receive this quantity of experience points.
  - "**finishesQuest**" with the value 1 or 0. 0 being the default, this field can be omitted entirely when this is the case. When set to 1, if the player reaches this quest stage, the quest is considered completed (hidden by default in the quest log), but other stages of the same quest can still be reached afterwards (more XP rewards, extra info in the quest log...).

**Full example using all fields.**
```
{
    "id":"quest_id",
    "name":"Quest Name",
    "showInLog":1,
    "stages":[
        {
            "progress":10,
            "logText":"Log text for step 10.\nCan be multi-line.",
            "rewardExperience":100
        },
        {
            "progress":20,
            "logText":"Log text for step 20",
            "rewardExperience":100,
            "finishesQuest":1
        }
    ]
}
```
