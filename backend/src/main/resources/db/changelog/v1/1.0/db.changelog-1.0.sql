create table public.playlist_archives
(
    id    varchar(50) not null
        primary key,
    title text,
    url   text,
    created_date timestamp
);

create table public.video_archives
(
    id           varchar(50) not null
        primary key,
    created_date timestamp,
    download_url text,
    progress     integer     not null,
    size_mb      double precision,
    status       varchar(255),
    title        text,
    youtube_url  text        not null
);

create table public.video_in_playlist
(
    playlist_id varchar(50) not null
        constraint playlist_fk_id
            references public.playlist_archives,
    video_id    varchar(50) not null
        constraint video_fk_id
            references public.video_archives
);