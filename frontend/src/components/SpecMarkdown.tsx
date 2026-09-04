import type { ComponentPropsWithoutRef } from 'react';
import ReactMarkdown from 'react-markdown';
import type { Components, ExtraProps } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';

type HeadingTag = 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';

/**
 * A specification body, rendered with the backend's own section anchors on its headings.
 *
 * <p>The anchors come from the backend, which parsed the document; deriving them here would be a
 * second implementation of the same rule, free to disagree with the one discussions, diffs and
 * approvals address. The document view and a review's Document tab share this component for the same
 * reason — two renderers would eventually give the same heading two different ids.
 */
export function SpecMarkdown({ content, anchors }: { content: string; anchors: string[] }) {
  // Fresh per render: a heading component must not keep state across renders, or a re-render of the
  // same content would double-count and diverge from the outline. A `const`-bound counter object
  // (mutated via a property, never reassigned) is recreated on every call to this render function,
  // so there is nothing to go stale between renders.
  const headingCursor = { next: 0 };
  function nextHeadingId(): string {
    const id = anchors[headingCursor.next] ?? `section-${headingCursor.next + 1}`;
    headingCursor.next += 1;
    return id;
  }
  function heading(Tag: HeadingTag) {
    return function Heading({ children }: ComponentPropsWithoutRef<HeadingTag> & ExtraProps) {
      return <Tag id={nextHeadingId()}>{children}</Tag>;
    };
  }
  const components: Components = {
    h1: heading('h1'),
    h2: heading('h2'),
    h3: heading('h3'),
    h4: heading('h4'),
    h5: heading('h5'),
    h6: heading('h6'),
  };

  return (
    <article className="doc" style={{ maxWidth: 'none' }}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]} components={components}>
        {content}
      </ReactMarkdown>
    </article>
  );
}
