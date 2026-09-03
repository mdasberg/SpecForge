import { EmptyState } from '../components/EmptyState';

export function Reviews() {
  return (
    <EmptyState title="No reviews yet">
      A review opens when a pull request touches a specification. Connect a repository to start.
    </EmptyState>
  );
}
