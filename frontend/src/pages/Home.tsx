import { EmptyState } from '../components/EmptyState';

export function Home() {
  return (
    <EmptyState title="Nothing to review yet">
      SpecForge shows your review queue here. Connect a repository to import its specifications.
    </EmptyState>
  );
}
