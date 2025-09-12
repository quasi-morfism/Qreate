# AI App Generator Frontend

A modern Vue 3 + TypeScript frontend for the AI App Generator platform. This application allows users to create, manage, and deploy applications through AI-powered conversations.

## Features

### For Users

- **AI-Powered App Creation**: Create applications by chatting with AI using natural language
- **Real-time Code Generation**: Watch as AI generates your app code in real-time with SSE streaming
- **Live Preview**: See your generated app immediately with built-in preview functionality
- **App Management**: View, edit, and manage your created applications
- **One-Click Deployment**: Deploy your applications with a single click
- **App Discovery**: Browse featured applications created by the community

### For Administrators

- **User Management**: Manage all users in the system
- **App Management**: Oversee all applications, set featured apps, and moderate content
- **Priority Control**: Set app priorities to feature exceptional applications

## Technology Stack

- **Frontend**: Vue 3 + TypeScript + Vite
- **UI Library**: Naive UI with custom green theme
- **Styling**: Modern CSS with responsive design
- **State Management**: Pinia for user state
- **Routing**: Vue Router with dynamic routes
- **Real-time Communication**: Server-Sent Events (SSE) for AI chat

## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Page Structure

### Main Pages

- **Home (`/`)**: Landing page with app creation interface and featured apps
- **App Generation (`/app/generate/:id`)**: Real-time AI chat interface for app development
- **App Detail (`/app/detail/:id`)**: View app information and preview
- **App Edit (`/app/edit/:id`)**: Edit app details (users can edit name, admins can edit all fields)

### Admin Pages

- **User Management (`/admin/user-manage`)**: Manage all users (admin only)
- **App Management (`/admin/app-manage`)**: Manage all applications (admin only)

### User Pages

- **Account Setup (`/user/account-setup`)**: User profile management

## Design Features

### Visual Design

- **Modern UI**: Clean, minimalist interface inspired by ChatGPT and Claude
- **Green Theme**: Professional green color scheme (#22c55e primary)
- **Responsive Design**: Works seamlessly on desktop, tablet, and mobile
- **Smooth Animations**: Subtle hover effects and transitions
- **Beautiful Gradients**: Eye-catching gradient backgrounds

### User Experience

- **Intuitive Navigation**: Clear navigation with user-friendly menu structure
- **Real-time Feedback**: Loading states, progress indicators, and instant feedback
- **Error Handling**: Comprehensive error handling with user-friendly messages
- **Accessibility**: Proper semantic HTML and keyboard navigation support

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
