# -*- coding: utf-8 -*-
from docx import Document

doc = Document(r"d:\Downloads\Коксюк_Диплом.docx")
for ti, t in enumerate(doc.tables):
    if not t.rows:
        continue
    first = " ".join(c.text for c in t.rows[0].cells)[:80]
    if "FR" in first or "ID" in first or "вимог" in first.lower():
        print(f"TABLE {ti}: {len(t.rows)}x{len(t.columns)} | {first}")
        for r in range(min(2, len(t.rows))):
            print(" ", [c.text[:30] for c in t.rows[r].cells])
