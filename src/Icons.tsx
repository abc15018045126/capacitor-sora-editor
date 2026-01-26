import React from 'react';

export const Icon = ({ d, size = 24, color = "currentColor", style = {} }: { d: string, size?: number, color?: string, style?: any }) => (
    <svg width={size} height={size} fill="none" stroke={color} strokeWidth="2" viewBox="0 0 24 24" style={style}>
        <path d={d} />
    </svg>
);
