import json
import glob

names_monster = set()
names_quest = set()


for filename in glob.glob("monster*.json"):
    with open(filename, "r", encoding="utf-8") as f:
        monsters = json.load(f)
    for monster in monsters:
        if "phraseID" in monster and "iconID" in monster:
            names_monster.add(monster["iconID"])
print(f"Number of different NPC with phraseID: {len(names_monster)}")


for filename in glob.glob("quest*.json"):
    with open(filename, "r", encoding="utf-8") as f:
        quests = json.load(f)
    for quest in quests:
        if quest.get("showInLog") == 1 and "id" in quest:
            names_quest.add(quest["id"])
print(f"Number of visible quests: {len(names_quest)}")