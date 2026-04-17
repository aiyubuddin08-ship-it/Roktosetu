import * as React from 'react';
import { ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      error: null
    };
  }

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      let errorMessage = "Something went wrong.";
      try {
        if (this.state.error?.message.startsWith('{')) {
          const detailedError = JSON.parse(this.state.error.message);
          errorMessage = `Database Error: ${detailedError.error || 'Permission denied'}`;
        }
      } catch (e) {
        // Fallback
      }

      return (
        <div className="flex flex-col items-center justify-center min-h-screen p-4 text-center bg-gray-50">
          <h2 className="text-2xl font-bold text-red-600 mb-2">Oops!</h2>
          <p className="text-gray-600 mb-4">{errorMessage}</p>
          <button
            className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
            onClick={() => window.location.reload()}
          >
            Refresh Page
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
