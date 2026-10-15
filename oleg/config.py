import os

from dotenv import load_dotenv

load_dotenv()

BOT_TOKEN = os.environ["BOT_TOKEN"]
OWNER_ID = int(os.environ["OWNER_ID"])
ANTHROPIC_API_KEY = os.environ.get("ANTHROPIC_API_KEY", "")
VISION_MODEL = os.environ.get("VISION_MODEL", "claude-haiku-5-5")
DB_PATH = os.environ.get("DB_PATH", "oleg.db")
