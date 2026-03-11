import clsx from 'clsx';

interface StatusBadgeProps {
  status: string;
  className?: string;
}

const statusStyles: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-700',
  UP: 'bg-green-100 text-green-700',
  INACTIVE: 'bg-gray-100 text-gray-600',
  LOCKED: 'bg-red-100 text-red-700',
  DOWN: 'bg-red-100 text-red-700',
  true: 'bg-green-100 text-green-700',
  false: 'bg-red-100 text-red-700',
};

export default function StatusBadge({ status, className }: StatusBadgeProps) {
  return (
    <span className={clsx(
      'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
      statusStyles[status] || 'bg-gray-100 text-gray-600',
      className
    )}>
      {status}
    </span>
  );
}
