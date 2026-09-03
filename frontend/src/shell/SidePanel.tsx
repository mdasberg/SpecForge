/**
 * The side panel is where a project and its specifications will be listed. It carries its own
 * empty state so the shell does not look broken before `add-spec-repository` fills it.
 */
export function SidePanel() {
  return (
    <aside className="side">
      <div>
        <div className="side-t">Projects</div>
        <div className="side-group">
          <div className="side-item" style={{ cursor: 'default' }}>
            No repository connected
          </div>
        </div>
      </div>
    </aside>
  );
}
