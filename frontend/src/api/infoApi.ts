import axiosClient from "./axiosConfig";

export const fetchVersionInfo = async () => {
    return (await axiosClient.get("/info/version")).data;
};
