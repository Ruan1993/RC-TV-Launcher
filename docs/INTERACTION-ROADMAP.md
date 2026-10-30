# Upcoming Phase B interaction requirements

These are recorded requirements, not implemented features in the Back/performance fix.

## App context menu

The permanent Pin/Unpin buttons under app cards are temporary. Replace them only after a tested long-press OK/Select interaction is available. The context menu must offer:

- Add Favourite / Remove Favourite, reflecting current state.
- Move / Rearrange, with predictable D-pad movement, commit and cancel.
- App Information through Android's application details screen.
- Uninstall through Android's own confirmation flow; never silently remove an app.

Preserve short-press Select to launch. Distinguish hold from key repeats, prevent launching after a handled long press, support Back to dismiss the menu before section navigation, and restore focus to its originating card. Validate rapid navigation, cancellation and accessibility on the Xiaomi remote before removing the temporary controls. Preserve existing favourites and preferences.

## RC Assist

Plan a future RC Assist menu entry and floating glass assistant surface, using a secure Gemini Flash backend. It should support questions, settings navigation and permission-based configuration changes. Keep backend credentials and API keys outside the APK. Actions must explain the intended operation and require the appropriate Android permission or user confirmation. No AI integration, network provider, background listener, new permission or working-looking menu entry is introduced by the Home redesign. Backend authentication, data handling and action scope require a later design review.
