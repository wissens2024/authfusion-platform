import clsx from 'clsx';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon?: React.ComponentType<{ className?: string }>;
  color?: 'blue' | 'green' | 'red' | 'yellow';
}

const colorMap = {
  blue: 'bg-blue-50 text-blue-600',
  green: 'bg-green-50 text-green-600',
  red: 'bg-red-50 text-red-600',
  yellow: 'bg-yellow-50 text-yellow-600',
};

export default function StatCard({ title, value, subtitle, icon: Icon, color = 'blue' }: StatCardProps) {
  return (
    <div className="card">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500">{title}</p>
          <p className="text-2xl font-bold mt-1">{value}</p>
          {subtitle && <p className="text-xs text-gray-400 mt-1">{subtitle}</p>}
        </div>
        {Icon && (
          <div className={clsx('p-3 rounded-lg', colorMap[color])}>
            <Icon className="w-6 h-6" />
          </div>
        )}
      </div>
    </div>
  );
}
