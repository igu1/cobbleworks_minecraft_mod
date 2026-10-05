# Cobbleworks publication files

- **`logo.png`** — 512 × 512 project avatar.
- **`banner.png`** — 1600 × 600 promotional banner.
- **`summary.txt`** — short, player-facing summary.
- **`description.html`** — player-facing description for CKEditor.

## Use the HTML description

Open `description.html` in a text editor, copy its contents, and paste into
CKEditor's **Source / HTML** mode if your editor exposes it. Switch back to the
visual editor to check the result before saving. Do not paste raw HTML into the
normal visual text field.

For a browser preview, open `../../branding/cobbleworks/README.html`.
If Source mode is unavailable, copy the rendered preview into the visual editor.
The site may remove some styling; headings, lists and readable text do not
depend on scripts or external stylesheets. Actual CurseForge rendering has not
been tested.

To include the banner in the description, upload it using the editor's image
tool. Local file paths are not usable by visitors, so none are embedded in the
HTML fragment.

The previous logo and banner are retained as `logo.previous.png` and
`banner.previous.png`. The existing Markdown description and machine screenshot
are unchanged. Editable SVG originals are in `../../branding/cobbleworks/`.
