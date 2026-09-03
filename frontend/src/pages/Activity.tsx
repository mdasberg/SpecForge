import { EmptyState } from '../components/EmptyState';

export function Activity() {
  return (
    <EmptyState title="No activity yet">
      Every approval, comment and check run appears here once a repository is connected.
    </EmptyState>
  );
}
