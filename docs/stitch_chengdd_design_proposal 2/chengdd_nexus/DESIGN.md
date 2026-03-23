```markdown
# Design System Document: The Precision Curator

## 1. Overview & Creative North Star
This design system is built for "程哒哒 ChengDD" to redefine the enterprise administrative experience. Moving away from the cluttered, "line-heavy" industrial standards, we adopt the Creative North Star: **"The Precision Curator."**

The goal is to treat data not as a chore, but as a premium asset. We achieve this through **high-end editorial layouts**, **tonal depth**, and **intentional whitespace**. By utilizing an asymmetric balance and sophisticated layering, we eliminate "visual noise," ensuring the user feels a sense of calm authority and absolute trust. This is where high-performance e-commerce utility meets the elegance of a modern digital atelier.

---

## 2. Color Strategy
Our palette balances the authoritative weight of Deep Blue-Grey with the high-energy pulse of Brand Orange.

### Core Brand Tokens
- **Primary (Action):** `primary_container` (#FF6B00) - Used for the most critical calls to action. It is high-visibility and energetic.
- **Deep Surface/Text:** `on_surface` (#091D2E) - Derived from the requested #2C3E50, this provides the "anchor" for typography and deep backgrounds.
- **The Neutral Base:** `background` (#F7F9FF) and `surface_container_lowest` (#FFFFFF) create a crisp, clean canvas.

### The "No-Line" Rule
To achieve a "low-noise" environment, **1px solid borders are strictly prohibited for sectioning.** 
- **Definition through Tone:** Boundaries must be defined by shifts in background color. For example, a sidebar should use `surface_container_low`, while the main workspace uses `surface`. 
- **Signature Textures:** For primary buttons or high-level headers, use a subtle linear gradient transitioning from `primary` (#A04100) to `primary_container` (#FF6B00) at a 135° angle. This adds "soul" and depth that flat hex codes lack.

---

## 3. Typography: The Editorial Voice
We use **Manrope** for its technical precision and modern humanist touch. It bridges the gap between a "trustworthy" corporate font and a "lightweight" e-commerce font.

- **Display & Headlines:** Use `display-sm` for hero numbers in statistics. The generous tracking and bold weights convey success.
- **Titles:** `title-lg` should be used for card headers. It provides a clear entry point for the eye.
- **Body:** `body-md` is the workhorse. Ensure a line-height of at least 1.6 for long-form data to maintain the "lightweight" feel.
- **Hierarchy through Contrast:** Instead of making everything bold, use `on_surface_variant` (#5A4136) for secondary labels to create a sophisticated grey-scale hierarchy against the deep blue-grey primary text.

---

## 4. Elevation & Depth: Tonal Layering
We reject the "flat" web. This design system treats the UI as a series of physical layers—like stacked sheets of fine paper or frosted glass.

- **The Layering Principle:**
    - **Base Level:** `surface` (#F7F9FF)
    - **Section Level:** `surface_container_low` (#EDF4FF)
    - **Content Card:** `surface_container_lowest` (#FFFFFF)
- **Ambient Shadows:** When an element must "float" (e.g., a dropdown or a modal), use a shadow with a 24px blur and 4% opacity, using the `on_surface` color as the shadow base. This creates a natural "glow" rather than a dirty smudge.
- **The "Ghost Border" Fallback:** If a separation is required for accessibility, use the `outline_variant` at **10% opacity**. It should be felt, not seen.
- **Glassmorphism:** For top navigation bars or floating filter panels, use `surface_container_lowest` with an 80% alpha and a `20px` backdrop blur. This allows the "Orange" and "Blue-Grey" accents to bleed through softly, integrating the layers.

---

## 5. Components

### 5.1 Statistics Cards
- **Structure:** No borders. Use `surface_container_lowest`.
- **Styling:** Large `display-sm` numbers in `on_surface`. Use a small `primary` (#A04100) sparkline or a micro-gradient icon to denote growth.
- **Layout:** Asymmetric. Place the label in the top-left (`label-md`) and the value in the bottom-right for an editorial feel.

### 5.2 Status Tags (Badges)
- **Design:** No "traffic light" extremes.
- **Draft:** `surface_container_high` background with `on_surface_variant` text.
- **On Sale (Active):** `primary_fixed` (#FFDBCC) background with `on_primary_fixed_variant` (#7A3000) text. 
- **Unlisted:** `error_container` (#FFDAD6) background with `on_error_container` (#93000A) text.
- **Shape:** Use the `full` (9999px) roundedness for tags to contrast against the `12px` cards.

### 5.3 Filterable Lists
- **The Filter Bar:** Use a "Glassmorphism" panel that sits at the top of the list.
- **Rows:** Forbid divider lines. Use `spacing-4` vertical padding. On hover, change the row background to `surface_container_low`.
- **Selection:** Use a `2px` vertical strip of `primary_container` on the far left of the row to indicate selection, rather than a checkbox only.

### 5.4 Clear Forms
- **Input Fields:** Use `surface_container_low` as the fill. 
- **States:** On focus, the background shifts to `surface_container_lowest` and a "Ghost Border" of `primary` appears.
- **Labels:** Always use `label-md` in `on_surface_variant`, positioned strictly above the input to reduce cognitive load.

---

## 6. Do's and Don'ts

### Do
- **Do** use whitespace as a structural element. If a layout feels "heavy," increase the spacing from `8` (2rem) to `12` (3rem).
- **Do** use the `DEFAULT` (12px) radius for cards, but feel free to use `xl` (24px) for large hero containers to soften the enterprise feel.
- **Do** ensure all "Orange" interactive elements have a contrast check against the `surface` background.

### Don't
- **Don't** use pure black (#000000). Use our `on_surface` (#091D2E) for all "black" needs to maintain the blue-grey sophisticated tone.
- **Don't** use purple or violet shades under any circumstances.
- **Don't** use 1px borders to separate table rows. Use alternating tonal shifts or simply generous whitespace.
- **Don't** overcrowd the dashboard. If an element doesn't serve a curation purpose, remove it. Low noise is the priority.

---
*End of Document. This system is designed to scale with ChengDD's growth, ensuring a premium, trustworthy experience at every touchpoint.*```