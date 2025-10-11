package com.example.ocr.screen.ocr.constant

val PROMPT_ANALYZE_SCHEDULE = """
      You are a vision model that reads staff shift tables.
      Task: Extract pairs of date and duty.
      Output only JSON that matches the provided schema.
      Rules:
      - Match each date with the corresponding duty on the same column/row.
      - Fixed field names: date, duty.
      - Preserve original date format (e.g., 09/01).
      - If unreadable, set form to "不明".
      - No extra content: no explanations, comments, additional text, key changes, or reordering
    """

val CHATGPT_MODEL = "gpt-4o"