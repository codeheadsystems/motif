import {
  Book,
  Briefcase,
  Heart,
  House,
  Palette,
  Tag,
  type LucideIcon,
} from 'lucide-react';

/**
 * Logical icon names (stored on Category.icon) → lucide-react components.
 * Keep this mapping small and intentional; unknown names fall back to Tag.
 */
const ICON_MAP: Record<string, LucideIcon> = {
  house: House,
  heart: Heart,
  palette: Palette,
  book: Book,
  briefcase: Briefcase,
  tag: Tag,
};

export function resolveIcon(name: string): LucideIcon {
  return ICON_MAP[name] ?? Tag;
}

export function CategoryIcon({
  name,
  className,
  size = 16,
}: {
  name: string;
  className?: string;
  size?: number;
}) {
  const Icon = resolveIcon(name);
  return <Icon size={size} className={className} aria-hidden />;
}
