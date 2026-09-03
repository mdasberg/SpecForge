import { EmptyState } from '../components/EmptyState';

export function Projects() {
  return (
    <EmptyState title="No projects yet">
      A project groups the specifications of one or more repositories. Connect a repository to
      create the first one.
    </EmptyState>
  );
}
